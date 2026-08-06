package me.tamkungz.nilkit.tooling;

import me.tamkungz.nilkit.tooling.bytebuddy.ByteBuddyPatch;
import me.tamkungz.nilkit.tooling.bytebuddy.ByteBuddyPatchRegistry;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;

import java.lang.instrument.Instrumentation;

/**
 * NilKit's Byte Buddy entry point.
 *
 * <p>For one-off generation, use {@link #byteBuddy()} or {@link #subclass(Class)}.
 * For Mixin-style class patching, register {@link ByteBuddyPatch} objects in
 * {@link #patches()} and install either the NilLoader bridge during premain or
 * an Instrumentation bridge.</p>
 *
 * <p>Nothing in the SDK startup path calls {@link ByteBuddyAgent#install()}.
 * Self-attachment remains explicit because many JVMs disable or restrict it.</p>
 */
public final class ByteBuddyHelper {

    /*
     * Keep the patching subsystem lazy. Simple Byte Buddy generation must not
     * initialize NilLoader-facing transformer classes as a side effect. This
     * also keeps makeSubclass()/byteBuddy() usable in tooling and unit tests
     * where NilLoader is intentionally absent from the runtime classpath.
     */
    private static final class PatchRegistryHolder {
        private static final ByteBuddyPatchRegistry INSTANCE = new ByteBuddyPatchRegistry();
    }

    private ByteBuddyHelper() {
    }

    /** Creates an unrestricted Byte Buddy entry point for advanced use. */
    public static ByteBuddy byteBuddy() {
        return new ByteBuddy();
    }

    /** Global composable patch registry used by the convenience methods below. */
    public static ByteBuddyPatchRegistry patches() {
        return PatchRegistryHolder.INSTANCE;
    }

    /** Registers a Mixin-style patch in the global registry. */
    public static ByteBuddyPatchRegistry register(ByteBuddyPatch patch) {
        return patches().register(patch);
    }

    /**
     * Installs the global registry into NilLoader's pre-load transformer chain.
     * Call during premain/hijack, before transformer registration is closed.
     */
    public static void installNilLoaderBridge() {
        patches().installNilLoaderBridge();
    }

    /**
     * Installs the global registry on existing Instrumentation. The installation
     * is retransformation-capable and can be reset through the returned handle.
     */
    public static ByteBuddyPatchRegistry.Installation installPatches(Instrumentation instrumentation) {
        return patches().installOn(instrumentation);
    }

    /**
     * Returns already-installed Byte Buddy instrumentation, or {@code null} if
     * the agent is not installed. This method never attempts self-attachment.
     */
    public static Instrumentation instrumentationOrNull() {
        try {
            return ByteBuddyAgent.getInstrumentation();
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Explicitly asks Byte Buddy to self-attach and returns Instrumentation.
     * Prefer instrumentation supplied by the loader/agent when available.
     */
    public static Instrumentation installInstrumentation() {
        return ByteBuddyAgent.install();
    }

    /** Returns true if Byte Buddy's agent is already installed. */
    public static boolean isInstrumentationAvailable() {
        return instrumentationOrNull() != null;
    }

    /** Starts a subclass builder without forcing callers to create ByteBuddy first. */
    public static <T> DynamicType.Builder<T> subclass(Class<T> baseType) {
        if (baseType == null) throw new IllegalArgumentException("baseType must not be null");
        return new ByteBuddy().subclass(baseType);
    }

    /** Generates and loads an empty subclass using a wrapper class loader. */
    public static <T> Class<? extends T> makeSubclass(Class<T> baseType) {
        if (baseType == null) throw new IllegalArgumentException("baseType must not be null");
        ClassLoader loader = baseType.getClassLoader();
        if (loader == null) loader = ClassLoader.getSystemClassLoader();
        return new ByteBuddy()
                .subclass(baseType)
                .make()
                .load(loader, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
    }
}
