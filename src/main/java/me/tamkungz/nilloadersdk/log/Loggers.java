package me.tamkungz.nilloadersdk.log;

import nilloader.api.NilLogger;

/**
 * Logger utility for NilLoaderSDK.
 *
 * Supports both global default root and per-mod/per-class explicit roots.
 */
public final class Loggers {

    /** Default logger root used by the SDK. */
    public static final String DEFAULT_ROOT = "NilLoaderSDK";

    /**
     * Mutable global fallback root.
     * Prefer explicit-root APIs for multi-mod environments.
     */
    private static volatile String root = DEFAULT_ROOT;

    private Loggers() {
    }

    private static String normalizeRoot(String input) {
        if (input == null) {
            return DEFAULT_ROOT;
        }
        String v = input.trim();
        return v.isEmpty() ? DEFAULT_ROOT : v;
    }

    /**
     * Sets global fallback logger root namespace.
     * If null/blank is provided, falls back to {@link #DEFAULT_ROOT}.
     */
    public static void setRoot(String newRoot) {
        root = normalizeRoot(newRoot);
    }

    /** Returns current global fallback logger root namespace. */
    public static String getRoot() {
        return root;
    }

    /** Resets global fallback logger root namespace back to default. */
    public static void resetRoot() {
        root = DEFAULT_ROOT;
    }

    /** Returns logger for the current global fallback root. */
    public static NilLogger sdk() {
        return NilLogger.get(root);
    }

    /** Returns logger for an explicit root (per-mod capable). */
    public static NilLogger sdk(String explicitRoot) {
        return NilLogger.get(normalizeRoot(explicitRoot));
    }

    /** Alias for per-mod root logger. */
    public static NilLogger forMod(String modId) {
        return sdk(modId);
    }

    /**
     * Returns a logger for the given class using global fallback root.
     * Falls back to root logger if type is null.
     */
    public static NilLogger forClass(Class<?> type) {
        if (type == null) {
            return sdk();
        }
        return NilLogger.get(root + "/" + type.getSimpleName());
    }

    /**
     * Returns a logger for the given class under explicit root (per-mod capable).
     * Falls back to explicit root logger if type is null.
     */
    public static NilLogger forClass(String explicitRoot, Class<?> type) {
        String resolved = normalizeRoot(explicitRoot);
        if (type == null) {
            return NilLogger.get(resolved);
        }
        return NilLogger.get(resolved + "/" + type.getSimpleName());
    }

    /** Alias for per-mod class logger. */
    public static NilLogger forModClass(String modId, Class<?> type) {
        return forClass(modId, type);
    }
}