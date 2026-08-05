package me.tamkungz.nilloadersdk.tooling;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lightweight capability checks for the optional libraries bundled in the
 * {@code -all.jar}. This class itself has no dependency on those libraries, so
 * it is safe to use with the normal NilLoaderSDK JAR as well.
 */
public final class DeveloperToolbox {

    private DeveloperToolbox() {
    }

    public static boolean hasByteBuddy() {
        return present("net.bytebuddy.ByteBuddy");
    }

    public static boolean hasByteBuddyAgent() {
        return present("net.bytebuddy.agent.ByteBuddyAgent");
    }

    public static boolean hasGeb() {
        return present("foo.zaaarf.geb.GEB");
    }

    public static boolean hasClassGraph() {
        return present("io.github.classgraph.ClassGraph");
    }

    public static boolean hasSnakeYaml() {
        return present("org.yaml.snakeyaml.Yaml");
    }

    /** Returns an immutable, deterministic capability map for diagnostics. */
    public static Map<String, Boolean> availability() {
        Map<String, Boolean> out = new LinkedHashMap<String, Boolean>();
        out.put("byte-buddy", hasByteBuddy());
        out.put("byte-buddy-agent", hasByteBuddyAgent());
        out.put("geb", hasGeb());
        out.put("classgraph", hasClassGraph());
        out.put("snakeyaml", hasSnakeYaml());
        return Collections.unmodifiableMap(out);
    }

    private static boolean present(String className) {
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        if (present(className, context)) return true;

        ClassLoader own = DeveloperToolbox.class.getClassLoader();
        return own != context && present(className, own);
    }

    private static boolean present(String className, ClassLoader loader) {
        try {
            Class.forName(className, false, loader);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
