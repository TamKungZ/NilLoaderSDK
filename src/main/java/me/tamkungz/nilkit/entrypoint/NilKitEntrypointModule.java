package me.tamkungz.nilkit.entrypoint;

/**
 * Java-first registration API for NilKit users.
 *
 * Implement this interface to register entrypoints using ServiceLoader.
 *
 * To use, create a file:
 * META-INF/services/me.tamkungz.nilkit.entrypoint.NilKitEntrypointModule
 * and list your implementation class.
 */
public interface NilKitEntrypointModule {

    default String getId() {
        return getClass().getName();
    }

    default void onPremain() throws Exception {}

    default void onHijack() throws Exception {}
}