package me.tamkungz.nilloadersdk.entrypoint;

/**
 * Java-first registration API for NilLoaderSDK users.
 *
 * Implement this interface to register entrypoints using ServiceLoader.
 *
 * To use, create a file:
 * META-INF/services/me.tamkungz.nilloadersdk.entrypoint.NilLoaderSDKEntrypointModule
 * and list your implementation class.
 */
public interface NilLoaderSDKEntrypointModule {

    default String getId() {
        return getClass().getName();
    }

    default void onPremain() throws Exception {}

    default void onHijack() throws Exception {}
}