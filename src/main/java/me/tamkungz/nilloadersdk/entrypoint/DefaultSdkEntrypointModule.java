package me.tamkungz.nilloadersdk.entrypoint;

import me.tamkungz.nilloadersdk.log.Loggers;
import me.tamkungz.nilloadersdk.metadata.KdlOnlyModBootstrapper;
import me.tamkungz.nilloadersdk.metadata.NilMetadataPatchInstaller;
import me.tamkungz.nilloadersdk.metadata.SdkDependencyEnforcer;
import me.tamkungz.nilloadersdk.network.MinecraftAutoNetworkBridge;
import nilloader.api.NilLogger;

/**
 * DefaultSdkEntrypointModule — built-in module for NilLoaderSDK.
 *
 * This module is automatically discovered via ServiceLoader and provides
 * default behavior for both premain and hijack phases.
 *
 * Responsibilities:
 * - Log lifecycle events
 * - Initialize MinecraftAutoNetworkBridge during hijack phase
 */
public final class DefaultSdkEntrypointModule implements NilLoaderSDKEntrypointModule {

    private static final NilLogger LOG = Loggers.sdk();

    @Override
    public String getId() {
        return "nilloadersdk-default-module";
    }

    @Override
    public void onPremain() {
        LOG.info("Default module onPremain");
        NilMetadataPatchInstaller.install();
        KdlOnlyModBootstrapper.bootstrapFromDefaultFolders();
        SdkDependencyEnforcer.enforceLoadedMods();
    }

    @Override
    public void onHijack() {
        LOG.info("Default module onHijack");
        MinecraftAutoNetworkBridge.startFromSystemProperties();
    }
}
