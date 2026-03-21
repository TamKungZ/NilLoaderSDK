package me.tamkungz.nilloadersdk.log;

import nilloader.api.NilLogger;

/**
 * Logger utility for NilLoaderSDK.
 *
 * Provides a shared root logger and per-class loggers.
 */
public final class Loggers {

    public static final String ROOT = "NilLoaderSDK";

    private Loggers() {
    }

    /** Returns the root SDK logger. */
    public static NilLogger sdk() {
        return NilLogger.get(ROOT);
    }

    /**
     * Returns a logger for the given class.
     * Falls back to root logger if type is null.
     */
    public static NilLogger forClass(Class<?> type) {
        if (type == null) {
            return sdk();
        }
        return NilLogger.get(ROOT + "/" + type.getSimpleName());
    }
}