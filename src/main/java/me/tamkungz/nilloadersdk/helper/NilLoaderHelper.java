package me.tamkungz.nilloadersdk.helper;

import me.tamkungz.nilloadersdk.metadata.SdkMetadataIO;
import me.tamkungz.nilloadersdk.metadata.SdkModMetadata;
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
     * Returns true if all provided mod ids are loaded.
     */
    public static boolean isAllModsLoaded(String... ids) {
        if (ids == null || ids.length == 0) return false;
        for (String id : ids) {
            if (!isModLoaded(id)) return false;
        }
        return true;
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
     * Returns the first loaded mod from the provided candidate ids.
     */
    public static Optional<NilMetadata> getFirstLoadedMod(String... candidateIds) {
        if (candidateIds == null || candidateIds.length == 0) return Optional.empty();
        for (String id : candidateIds) {
            Optional<NilMetadata> mod = getModMetadata(id);
            if (mod.isPresent()) return mod;
        }
        return Optional.empty();
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
     * Returns all entrypoint phase->class mappings for the given mod id.
     */
    public static Map<String, String> getEntrypoints(String id) {
        NilMetadata mod = getModMetadataOrNull(id);
        if (mod == null || mod.entrypoints == null || mod.entrypoints.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<String, String>(mod.entrypoints));
    }

    /**
     * Returns true if a mod declares an entrypoint for a given phase.
     */
    public static boolean hasEntrypoint(String id, String phase) {
        return getEntrypointClass(id, phase) != null;
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

    /**
     * Reads SDK-only metadata for a loaded mod by id from `.nilsdkmod.kdl`
     * (fallback: legacy `.kdl` names).
     *
     * <p>This is optional metadata and does not affect base NilLoader metadata parsing.</p>
     */
    public static Optional<SdkModMetadata> getSdkMetadata(String id) {
        NilMetadata mod = getModMetadataOrNull(id);
        if (mod == null) return Optional.empty();
        return getSdkMetadata(mod);
    }

    /**
     * Reads SDK-only metadata from a {@link NilMetadata#source} location.
     */
    public static Optional<SdkModMetadata> getSdkMetadata(NilMetadata mod) {
        if (mod == null || mod.source == null) return Optional.empty();
        return SdkMetadataIO.readFromSource(mod.source, mod.id);
    }

    /**
     * Returns list of missing required mod ids declared in SDK metadata.
     */
    public static List<String> getMissingRequiredMods(String id) {
        Optional<SdkModMetadata> metadata = getSdkMetadata(id);
        if (!metadata.isPresent()) return Collections.emptyList();

        List<String> missing = new ArrayList<String>();
        for (String dep : metadata.get().getRequiredMods()) {
            if (!isModLoaded(dep)) missing.add(dep);
        }
        return Collections.unmodifiableList(missing);
    }

    /**
     * Returns true if this mod is currently missing any required dependencies.
     */
    public static boolean hasMissingRequiredMods(String id) {
        return !getMissingRequiredMods(id).isEmpty();
    }

    /**
     * True when all required SDK metadata dependencies are currently loaded.
     */
    public static boolean areRequiredModsLoaded(String id) {
        return getMissingRequiredMods(id).isEmpty();
    }

    /**
     * Returns ids this mod prefers to load before (advisory metadata).
     */
    public static List<String> getLoadBefore(String id) {
        Optional<SdkModMetadata> metadata = getSdkMetadata(id);
        return metadata.isPresent() ? metadata.get().getLoadBefore() : Collections.<String>emptyList();
    }

    /**
     * Returns ids this mod prefers to load after (advisory metadata).
     */
    public static List<String> getLoadAfter(String id) {
        Optional<SdkModMetadata> metadata = getSdkMetadata(id);
        return metadata.isPresent() ? metadata.get().getLoadAfter() : Collections.<String>emptyList();
    }

    /**
     * Returns icon path from SDK metadata, or null if not declared.
     */
    public static String getIconPath(String id) {
        Optional<SdkModMetadata> metadata = getSdkMetadata(id);
        return metadata.isPresent() ? metadata.get().getIcon() : null;
    }

    /**
     * Returns mod page URL from SDK metadata, or null if not declared.
     */
    public static String getModUrl(String id) {
        Optional<SdkModMetadata> metadata = getSdkMetadata(id);
        return metadata.isPresent() ? metadata.get().getModUrl() : null;
    }

    /**
     * Returns source repository URL from SDK metadata, or null if not declared.
     */
    public static String getSourceUrl(String id) {
        Optional<SdkModMetadata> metadata = getSdkMetadata(id);
        return metadata.isPresent() ? metadata.get().getSourceUrl() : null;
    }

    /**
     * Returns license string from SDK metadata, or null if not declared.
     */
    public static String getLicense(String id) {
        Optional<SdkModMetadata> metadata = getSdkMetadata(id);
        return metadata.isPresent() ? metadata.get().getLicense() : null;
    }

    /**
     * Returns credits/contributors from SDK metadata.
     */
    public static List<String> getCredits(String id) {
        Optional<SdkModMetadata> metadata = getSdkMetadata(id);
        return metadata.isPresent() ? metadata.get().getCredits() : Collections.<String>emptyList();
    }

    /**
     * Returns required mod ids declared in SDK metadata.
     */
    public static List<String> getRequiredMods(String id) {
        Optional<SdkModMetadata> metadata = getSdkMetadata(id);
        return metadata.isPresent() ? metadata.get().getRequiredMods() : Collections.<String>emptyList();
    }

    /**
     * Whether this mod should soft-fail on missing dependencies (default true).
     */
    public static boolean isSafeLoad(String id) {
        Optional<SdkModMetadata> metadata = getSdkMetadata(id);
        return !metadata.isPresent() || metadata.get().isSafeLoad();
    }

    /**
     * Easy icon map for UI integrations (e.g., mod menu).
     */
    public static Map<String, String> getLoadedModIcons() {
        Map<String, String> out = new LinkedHashMap<String, String>();
        for (NilMetadata mod : getAllLoadedMods()) {
            if (mod == null || mod.id == null) continue;
            String icon = getIconPath(mod.id);
            if (icon != null) out.put(mod.id, icon);
        }
        return Collections.unmodifiableMap(out);
    }

    /**
     * Returns loaded mod ids that have declared an entrypoint for the given phase.
     */
    public static List<String> getModsWithEntrypoint(String phase) {
        if (phase == null || phase.trim().isEmpty()) return Collections.emptyList();
        List<String> out = new ArrayList<String>();
        for (NilMetadata mod : getAllLoadedMods()) {
            if (mod == null || mod.id == null) continue;
            if (hasEntrypoint(mod.id, phase)) out.add(mod.id);
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * Returns dependency report of loaded mods currently missing required dependencies.
     *
     * <p>Map key is loaded mod id, map value is list of missing required ids.</p>
     */
    public static Map<String, List<String>> getMissingRequiredModsForLoadedMods() {
        Map<String, List<String>> out = new LinkedHashMap<String, List<String>>();
        for (NilMetadata mod : getAllLoadedMods()) {
            if (mod == null || mod.id == null) continue;
            List<String> missing = getMissingRequiredMods(mod.id);
            if (!missing.isEmpty()) out.put(mod.id, missing);
        }
        return Collections.unmodifiableMap(out);
    }

    /**
     * Returns loaded mod ids that declare the given mod id as a required dependency.
     */
    public static List<String> getModsRequiring(String requiredModId) {
        if (requiredModId == null || requiredModId.trim().isEmpty()) return Collections.emptyList();
        String target = requiredModId.trim();

        List<String> out = new ArrayList<String>();
        for (NilMetadata mod : getAllLoadedMods()) {
            if (mod == null || mod.id == null) continue;
            List<String> deps = getRequiredMods(mod.id);
            if (deps.contains(target)) out.add(mod.id);
        }
        return Collections.unmodifiableList(out);
    }
}
