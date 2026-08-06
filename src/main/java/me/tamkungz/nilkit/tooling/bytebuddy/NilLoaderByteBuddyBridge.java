package me.tamkungz.nilkit.tooling.bytebuddy;

import nilloader.api.ClassTransformer;

/**
 * NilLoader-specific adapter for {@link ByteBuddyPatchRegistry}.
 *
 * <p>This class intentionally contains all hard references to NilLoader used by
 * the Byte Buddy patch registry. It is loaded only when the NilLoader bridge is
 * explicitly installed, so Byte Buddy generation and Instrumentation mode do
 * not require NilLoader on their runtime classpath.</p>
 */
public final class NilLoaderByteBuddyBridge {

    private NilLoaderByteBuddyBridge() {
    }

    public static void install(final ByteBuddyPatchRegistry registry) {
        if (registry == null) throw new IllegalArgumentException("registry cannot be null");

        ClassTransformer.register(new ClassTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String internalClassName, byte[] originalData) {
                if (originalData == null || registry.isEmpty()) return originalData;

                String binaryName = internalClassName.replace('/', '.');
                try {
                    return registry.transformBytes(loader, binaryName, originalData);
                } catch (Throwable t) {
                    throw new RuntimeException(
                            "Byte Buddy NilLoader transformation failed for " + binaryName,
                            t);
                }
            }

            @Override
            public byte[] transform(String className, byte[] originalData) {
                return transform(ClassLoader.getSystemClassLoader(), className, originalData);
            }
        });
    }
}
