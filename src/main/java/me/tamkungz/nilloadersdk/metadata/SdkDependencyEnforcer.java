package me.tamkungz.nilloadersdk.metadata;

import me.tamkungz.nilloadersdk.log.Loggers;
import nilloader.api.NilLogger;
import nilloader.api.NilMetadata;
import nilloader.api.NilModList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SdkDependencyEnforcer {

    private static final NilLogger LOG = Loggers.sdk();

    private SdkDependencyEnforcer() {}

    public static void enforceLoadedMods() {
        List<NilMetadata> mods = NilModList.getAll();
        if (mods == null || mods.isEmpty()) return;

        for (NilMetadata mod : mods) {
            if (mod == null || mod.id == null || mod.source == null) continue;

            Optional<SdkModMetadata> metadata = SdkMetadataIO.readFromSource(mod.source, mod.id);
            if (!metadata.isPresent()) continue;

            SdkModMetadata m = metadata.get();
            if (m.getRequiredMods().isEmpty()) continue;

            List<String> missing = new ArrayList<String>();
            for (String dep : m.getRequiredMods()) {
                if (dep == null || dep.trim().isEmpty()) continue;
                if (!NilModList.isLoaded(dep.trim())) missing.add(dep.trim());
            }

            if (missing.isEmpty()) continue;

            String msg = "Missing required mods for " + mod.id + ": " + missing + " (safeLoad=" + m.isSafeLoad() + ")";
            if (m.isSafeLoad()) {
                LOG.warn(msg);
            } else {
                LOG.error(msg);
                throw new IllegalStateException(msg);
            }
        }
    }
}

