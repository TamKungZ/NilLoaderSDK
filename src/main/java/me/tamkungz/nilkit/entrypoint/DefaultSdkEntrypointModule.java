package me.tamkungz.nilkit.entrypoint;

import me.tamkungz.nilkit.log.Loggers;
import me.tamkungz.nilkit.metadata.KdlOnlyModBootstrapper;
import me.tamkungz.nilkit.metadata.NilMetadataPatchInstaller;
import me.tamkungz.nilkit.metadata.SdkDependencyEnforcer;
import me.tamkungz.nilkit.network.MinecraftAutoNetworkBridge;
import nilloader.api.NilLogger;

/**
 * DefaultSdkEntrypointModule — built-in module for NilKit.
 *
 * This module is automatically discovered via ServiceLoader and provides
 * default behavior for both premain and hijack phases.
 *
 * Responsibilities:
 * - Log lifecycle events
 * - Initialize MinecraftAutoNetworkBridge during hijack phase
 */
public final class DefaultSdkEntrypointModule implements NilKitEntrypointModule {

    private static final NilLogger LOG = Loggers.sdk();

    @Override
    public String getId() {
        return "nilkit-default-module";
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
