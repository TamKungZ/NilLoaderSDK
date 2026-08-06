package me.tamkungz.nilkit.tooling.bytebuddy;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.ResettableClassFileTransformer;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.JavaModule;

import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Composes and installs {@link ByteBuddyPatch} instances.
 *
 * <p>Two execution paths are supported:</p>
 * <ul>
 *   <li>NilLoader pre-load transformation, requiring no self-attach.</li>
 *   <li>Instrumentation/AgentBuilder with retransformation for already loaded classes.</li>
 * </ul>
 *
 * <p>This makes the same patch definition usable both like a traditional Mixin
 * (before class definition) and like a runtime agent when Instrumentation exists.</p>
 */
public final class ByteBuddyPatchRegistry {

    private final CopyOnWriteArrayList<ByteBuddyPatch> patches = new CopyOnWriteArrayList<ByteBuddyPatch>();
    private final AtomicBoolean nilLoaderBridgeInstalled = new AtomicBoolean(false);

    public ByteBuddyPatchRegistry register(ByteBuddyPatch patch) {
        if (patch == null) throw new IllegalArgumentException("patch cannot be null");
        for (ByteBuddyPatch existing : patches) {
            if (existing.getId().equals(patch.getId())) {
                throw new IllegalArgumentException("Duplicate Byte Buddy patch id: " + patch.getId());
            }
        }
        patches.add(patch);
        return this;
    }

    public boolean unregister(String id) {
        if (id == null) return false;
        for (ByteBuddyPatch patch : patches) {
            if (id.equals(patch.getId())) {
                return patches.remove(patch);
            }
        }
        return false;
    }

    public void clear() {
        patches.clear();
    }

    public List<ByteBuddyPatch> snapshot() {
        return Collections.unmodifiableList(sortedSnapshot());
    }

    public boolean isEmpty() {
        return patches.isEmpty();
    }

    /**
     * Installs one catch-all NilLoader transformer. Register this during
     * premain/hijack, before NilLoader closes transformer registration.
     *
     * <p>Patches themselves remain dynamic: a patch registered later still
     * affects classes that have not yet been defined.</p>
     */
    public void installNilLoaderBridge() {
        if (!nilLoaderBridgeInstalled.compareAndSet(false, true)) return;

        try {
            /*
             * Deliberately load the NilLoader adapter reflectively. Keeping the
             * nilloader.api.ClassTransformer reference in a separate class means
             * this registry remains loadable for Byte Buddy-only tooling and for
             * Instrumentation agents whose runtime does not contain NilLoader.
             */
            ClassLoader loader = ByteBuddyPatchRegistry.class.getClassLoader();
            Class<?> bridge = Class.forName(
                    "me.tamkungz.nilkit.tooling.bytebuddy.NilLoaderByteBuddyBridge",
                    true,
                    loader);
            java.lang.reflect.Method install = bridge.getDeclaredMethod(
                    "install", ByteBuddyPatchRegistry.class);
            install.invoke(null, this);
        } catch (Throwable t) {
            nilLoaderBridgeInstalled.set(false);

            Throwable cause = t;
            if (t instanceof java.lang.reflect.InvocationTargetException
                    && ((java.lang.reflect.InvocationTargetException) t).getCause() != null) {
                cause = ((java.lang.reflect.InvocationTargetException) t).getCause();
            }

            throw new IllegalStateException(
                    "Cannot install the NilLoader Byte Buddy bridge. "
                            + "Ensure NilLoader is present and transformer registration is still open.",
                    cause);
        }
    }

    /**
     * Transforms a raw class file without loading the class.
     * Useful for NilLoader and for tests/tools that patch byte arrays directly.
     */
    public byte[] transformBytes(ClassLoader loader, String binaryClassName, byte[] originalBytes) {
        if (binaryClassName == null) throw new IllegalArgumentException("binaryClassName cannot be null");
        if (originalBytes == null) throw new IllegalArgumentException("originalBytes cannot be null");

        final String name = binaryClassName.replace('/', '.');
        ClassFileLocator primary = ClassFileLocator.Simple.of(name, originalBytes);
        ClassFileLocator fallback = loader == null
                ? ClassFileLocator.ForClassLoader.ofBootLoader()
                : ClassFileLocator.ForClassLoader.of(loader);
        ClassFileLocator locator = new ClassFileLocator.Compound(primary, fallback);

        TypeDescription type = TypePool.Default.of(locator).describe(name).resolve();
        List<ByteBuddyPatch> matches = matching(type);
        if (matches.isEmpty()) return originalBytes;

        DynamicType.Builder<?> builder = new ByteBuddy()
                .with(MethodGraph.Compiler.Default.forJavaHierarchy())
                .redefine(type, locator);

        for (ByteBuddyPatch patch : matches) {
            builder = patch.apply(type, builder);
        }

        return builder.make().getBytes();
    }

    /**
     * Installs all registered patches as one retransformation-capable Byte Buddy agent.
     * The returned handle can later be reset.
     */
    public Installation installOn(final Instrumentation instrumentation) {
        if (instrumentation == null) throw new IllegalArgumentException("instrumentation cannot be null");

        AgentBuilder builder = new AgentBuilder.Default(new ByteBuddy()
                .with(MethodGraph.Compiler.Default.forJavaHierarchy()))
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .disableClassFormatChanges()
                .type(new AgentBuilder.RawMatcher() {
                    @Override
                    public boolean matches(TypeDescription typeDescription,
                                           ClassLoader classLoader,
                                           JavaModule module,
                                           Class<?> classBeingRedefined,
                                           ProtectionDomain protectionDomain) {
                        return !matching(typeDescription).isEmpty();
                    }
                })
                .transform(new AgentBuilder.Transformer() {
                    @Override
                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> dynamicBuilder,
                                                            TypeDescription typeDescription,
                                                            ClassLoader classLoader,
                                                            JavaModule module,
                                                            ProtectionDomain protectionDomain) {
                        DynamicType.Builder<?> current = dynamicBuilder;
                        for (ByteBuddyPatch patch : matching(typeDescription)) {
                            current = patch.apply(typeDescription, current);
                        }
                        return current;
                    }
                });

        ResettableClassFileTransformer transformer = builder.installOn(instrumentation);
        return new Installation(instrumentation, transformer);
    }

    private List<ByteBuddyPatch> matching(TypeDescription type) {
        List<ByteBuddyPatch> result = new ArrayList<ByteBuddyPatch>();
        for (ByteBuddyPatch patch : sortedSnapshot()) {
            if (patch.matches(type)) result.add(patch);
        }
        return result;
    }

    private List<ByteBuddyPatch> sortedSnapshot() {
        List<ByteBuddyPatch> result = new ArrayList<ByteBuddyPatch>(patches);
        Collections.sort(result, new Comparator<ByteBuddyPatch>() {
            @Override
            public int compare(ByteBuddyPatch a, ByteBuddyPatch b) {
                int priority = Integer.compare(a.getPriority(), b.getPriority());
                return priority != 0 ? priority : a.getId().compareTo(b.getId());
            }
        });
        return result;
    }

    public static final class Installation implements AutoCloseable {
        private final Instrumentation instrumentation;
        private final ResettableClassFileTransformer transformer;
        private final AtomicBoolean closed = new AtomicBoolean(false);

        private Installation(Instrumentation instrumentation, ResettableClassFileTransformer transformer) {
            this.instrumentation = instrumentation;
            this.transformer = transformer;
        }

        public boolean reset() {
            if (!closed.compareAndSet(false, true)) return false;
            return transformer.reset(instrumentation, AgentBuilder.RedefinitionStrategy.RETRANSFORMATION);
        }

        @Override
        public void close() {
            reset();
        }
    }
}
