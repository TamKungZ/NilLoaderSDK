package me.tamkungz.nilkit.tooling;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;

import java.lang.instrument.Instrumentation;

/**
 * Convenience entry points for the Byte Buddy libraries bundled by the
 * NilKit all-in-one artifact.
 *
 * <p>Nothing in the SDK startup path calls {@link ByteBuddyAgent#install()}.
 * Self-attachment is explicit because some JVMs disable or restrict it.</p>
 */
public final class ByteBuddyHelper {

    private ByteBuddyHelper() {
    }

    /** Creates a normal Byte Buddy builder entry point for advanced use. */
    public static ByteBuddy byteBuddy() {
        return new ByteBuddy();
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
     * Callers should prefer instrumentation supplied by their loader/agent when
     * available. Self-attachment is JVM/platform dependent and may be disabled;
     * when Byte Buddy is repackaged inside a shadow JAR, upstream attach behavior
     * is also not guaranteed on every VM. Any failure is intentionally propagated
     * to the caller rather than affecting NilKit startup.
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

    /**
     * Generates and loads an empty subclass using a wrapper class loader.
     * Useful for quick prototypes; advanced transformations should use
     * {@link #subclass(Class)} directly.
     */
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
