package me.tamkungz.nilloadersdk.helper;

import nilloader.api.NilMetadata;
import nilloader.api.NilModList;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * NilLoaderHelper — convenience wrapper around NilLoader mod metadata APIs.
 *
 * <p>This helper centralizes common mod-list and metadata operations that are
 * frequently used by SDK-based mods, so each mod does not need to re-implement
 * boilerplate logic around {@link NilModList} / {@link NilMetadata}.</p>
 */
public final class NilLoaderHelper {

    private NilLoaderHelper() {}

    /**
     * Returns true if a mod with the given id is loaded.
     */
    public static boolean isModLoaded(String id) {
        if (id == null || id.trim().isEmpty()) return false;
        return NilModList.isLoaded(id.trim());
    }

    /**
     * Returns true if at least one provided mod id is loaded.
     */
    public static boolean isAnyModLoaded(String... ids) {
        if (ids == null || ids.length == 0) return false;
        for (String id : ids) {
            if (isModLoaded(id)) return true;
        }
        return false;
    }

    /**
     * Returns metadata for a mod id.
     */
    public static Optional<NilMetadata> getModMetadata(String id) {
        if (id == null || id.trim().isEmpty()) return Optional.empty();
        return NilModList.getById(id.trim());
    }

    /**
     * Safe metadata lookup that returns null when unavailable.
     */
    public static NilMetadata getModMetadataOrNull(String id) {
        Optional<NilMetadata> found = getModMetadata(id);
        return found.orElse(null);
    }

    /**
     * Returns all loaded mod metadata entries.
     */
    public static List<NilMetadata> getAllLoadedMods() {
        List<NilMetadata> all = NilModList.getAll();
        if (all == null || all.isEmpty()) return Collections.emptyList();
        return Collections.unmodifiableList(new ArrayList<>(all));
    }

    /**
     * Returns all loaded mod ids.
     */
    public static List<String> getLoadedModIds() {
        List<String> out = new ArrayList<>();
        for (NilMetadata mod : getAllLoadedMods()) {
            if (mod != null && mod.id != null) out.add(mod.id);
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * Returns all loaded mod names.
     */
    public static List<String> getLoadedModNames() {
        List<String> out = new ArrayList<>();
        for (NilMetadata mod : getAllLoadedMods()) {
            if (mod == null) continue;
            if (mod.name != null && !mod.name.trim().isEmpty()) out.add(mod.name);
            else if (mod.id != null) out.add(mod.id);
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * Returns a map of loaded mods by id, preserving load iteration order.
     */
    public static Map<String, NilMetadata> getLoadedModsById() {
        Map<String, NilMetadata> out = new LinkedHashMap<>();
        for (NilMetadata mod : getAllLoadedMods()) {
            if (mod == null || mod.id == null) continue;
            out.put(mod.id, mod);
        }
        return Collections.unmodifiableMap(out);
    }

    /**
     * Returns the source file (JAR/path) of the given mod id.
     */
    public static File getSourceFile(String id) {
        NilMetadata mod = getModMetadataOrNull(id);
        return mod != null ? mod.source : null;
    }

    /**
     * Returns declared entrypoint names for the given mod id.
     */
    public static Set<String> getEntrypointNames(String id) {
        NilMetadata mod = getModMetadataOrNull(id);
        if (mod == null || mod.entrypoints == null || mod.entrypoints.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new LinkedHashSet<>(mod.entrypoints.keySet()));
    }

    /**
     * Returns a specific entrypoint target class for a given phase.
     */
    public static String getEntrypointClass(String id, String phase) {
        if (phase == null || phase.trim().isEmpty()) return null;
        NilMetadata mod = getModMetadataOrNull(id);
        if (mod == null || mod.entrypoints == null) return null;
        return mod.entrypoints.get(phase.trim());
    }

    /**
     * Returns a compact single-line description suitable for logs.
     */
    public static String describeMod(String id) {
        NilMetadata mod = getModMetadataOrNull(id);
        if (mod == null) return "<missing:" + id + ">";

        String modId = mod.id != null ? mod.id : "?";
        String name = mod.name != null ? mod.name : modId;
        String version = mod.version != null ? mod.version : "?";
        String authors = mod.authors != null ? mod.authors : "?";
        return name + " (id=" + modId + ", version=" + version + ", authors=" + authors + ")";
    }
}
