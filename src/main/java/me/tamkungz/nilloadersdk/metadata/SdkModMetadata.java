package me.tamkungz.nilloadersdk.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SDK-only mod metadata loaded from a dedicated .kdl file.
 *
 * <p>This metadata is intentionally separate from NilLoader's base nilmod metadata
 * so vanilla NilLoader can ignore it safely when this SDK is not present.</p>
 */
public final class SdkModMetadata {

    private final List<String> requiredMods;
    private final List<String> loadBefore;
    private final List<String> loadAfter;
    private final String icon;
    private final boolean safeLoad;

    public SdkModMetadata(List<String> requiredMods, List<String> loadBefore, List<String> loadAfter, String icon, boolean safeLoad) {
        this.requiredMods = immutableCopy(requiredMods);
        this.loadBefore = immutableCopy(loadBefore);
        this.loadAfter = immutableCopy(loadAfter);
        this.icon = normalize(icon);
        this.safeLoad = safeLoad;
    }

    public static SdkModMetadata empty() {
        return new SdkModMetadata(Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), null, true);
    }

    public List<String> getRequiredMods() {
        return requiredMods;
    }

    public List<String> getLoadBefore() {
        return loadBefore;
    }

    public List<String> getLoadAfter() {
        return loadAfter;
    }

    public String getIcon() {
        return icon;
    }

    public boolean isSafeLoad() {
        return safeLoad;
    }

    public boolean isEmpty() {
        return requiredMods.isEmpty() && loadBefore.isEmpty() && loadAfter.isEmpty() && icon == null && safeLoad;
    }

    private static List<String> immutableCopy(List<String> in) {
        if (in == null || in.isEmpty()) return Collections.emptyList();
        List<String> out = new ArrayList<String>();
        for (String s : in) {
            String n = normalize(s);
            if (n != null && !out.contains(n)) out.add(n);
        }
        return Collections.unmodifiableList(out);
    }

    private static String normalize(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}

