package me.tamkungz.nilloadersdk.helper;

import nilloader.api.ASMTransformer;
import nilloader.api.ClassTransformer;
import nilloader.api.lib.asm.tree.ClassNode;

/**
 * TransformerHelper — lightweight class patch utilities for NilLoader.
 *
 * <p>Designed for cases where you cannot use Mixin, but still want
 * Java-agent style class mutation via NilLoader's transformer pipeline.</p>
 *
 * <p>Important: register transformers during premain/hijack entrypoint.
 * Registering too late will throw from NilLoader.</p>
 */
public final class TransformerHelper {

    private TransformerHelper() {}

    /**
     * Functional callback for byte[] class patching.
     */
    public interface BytecodePatch {
        byte[] apply(ClassLoader loader, String internalClassName, byte[] originalBytes) throws Exception;
    }

    /**
     * Functional callback for ASM tree patching.
     */
    public interface AsmPatch {
        /**
         * @return true when frame recomputation is needed.
         */
        boolean apply(ClassLoader loader, ClassNode classNode) throws Exception;
    }

    /**
     * Register a simple raw-bytecode patch for one class.
     *
     * @param className dot or slash notation is accepted.
     */
    public static void registerBytecodePatch(final String className, final BytecodePatch patch) {
        if (patch == null) throw new IllegalArgumentException("patch cannot be null");
        final String target = normalizeClassName(className);

        ClassTransformer.register(new ClassTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String internalClassName, byte[] originalData) {
                if (!target.equals(internalClassName)) return originalData;
                try {
                    byte[] out = patch.apply(loader, internalClassName, originalData);
                    return out != null ? out : originalData;
                } catch (Throwable t) {
                    throw new RuntimeException("Failed bytecode patch for " + internalClassName, t);
                }
            }

            @Override
            public byte[] transform(String className, byte[] originalData) {
                return transform(ClassLoader.getSystemClassLoader(), className, originalData);
            }
        });
    }

    /**
     * Register an ASM ClassNode patch for one class.
     *
     * @param className dot or slash notation is accepted.
     */
    public static void registerAsmPatch(final String className, final AsmPatch patch) {
        if (patch == null) throw new IllegalArgumentException("patch cannot be null");
        final String target = normalizeClassName(className);

        ClassTransformer.register(new ASMTransformer() {
            @Override
            public boolean canTransform(ClassLoader loader, String internalClassName) {
                return target.equals(internalClassName);
            }

            @Override
            public boolean transform(ClassLoader loader, ClassNode classNode) {
                try {
                    return patch.apply(loader, classNode);
                } catch (Throwable t) {
                    throw new RuntimeException("Failed ASM patch for " + classNode.name, t);
                }
            }
        });
    }

    /**
     * True if two class names match after normalizing to internal slash format.
     */
    public static boolean classNameEquals(String a, String b) {
        return normalizeClassName(a).equals(normalizeClassName(b));
    }

    /**
     * Converts class names to internal slash format expected by transformers.
     */
    public static String normalizeClassName(String className) {
        if (className == null) throw new IllegalArgumentException("className cannot be null");
        String s = className.trim();
        if (s.isEmpty()) throw new IllegalArgumentException("className cannot be blank");
        return s.replace('.', '/');
    }
}
