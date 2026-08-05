package me.tamkungz.nilloadersdk.metadata;

import me.tamkungz.nilloadersdk.log.Loggers;
import nilloader.api.NilLogger;
import nilloader.api.NilMetadata;
import nilloader.api.NilModList;
import nilloader.api.lib.qdcss.QDCSS;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Boots KDL-only mods (no *.nilmod.css) into NilLoader runtime during SDK premain.
 *
 * <p>Why this exists:
 * NilLoader discovers mods by root-level <code>*.nilmod.css</code>. KDL-only mods are
 * otherwise skipped. Since SDK itself is already loaded via CSS, we can discover
 * KDL-only jars and inject their metadata/listeners reflectively.</p>
 */
public final class KdlOnlyModBootstrapper {

    private static final NilLogger LOG = Loggers.sdk("ModBootstrapper");
    private static final String KDL_SUFFIX = ".nilsdkmod.kdl";
    private static final String DEFAULT_KDL_NAME = "nilloadersdk.nilsdkmod.kdl";
    private static volatile boolean bootstrapped;

    private KdlOnlyModBootstrapper() {}

    public static synchronized void bootstrapFromDefaultFolders() {
        if (bootstrapped) return;
        bootstrapped = true;

        LOG.info("Starting KDL-only bootstrap pass");

        List<String> loaded = new ArrayList<String>();
        int candidates = 0;

        candidates += bootstrapFromDirectory(new File("mods"), loaded);
        candidates += bootstrapFromDirectory(new File("nilmods"), loaded);

        if (loaded.isEmpty()) {
            LOG.info("KDL-only bootstrap pass complete: no mods injected (candidates scanned={})", candidates);
        } else {
            LOG.info("KDL-only bootstrap pass complete: injected {} mod(s) from {} candidate archive(s): {}",
                    loaded.size(), candidates, loaded);
        }

        logLoadedModsTable();
    }

    private static void logLoadedModsTable() {
        List<NilMetadata> mods = NilModList.getAll();
        if (mods == null || mods.isEmpty()) {
            LOG.info("Loaded mods table: <empty>");
            return;
        }

        int idW = "ID".length();
        int nameW = "Name".length();
        int versionW = "Version".length();
        int authorsW = "Authors".length();
        int licenseW = "License".length();

        List<String[]> rows = new ArrayList<String[]>();
        for (NilMetadata mod : mods) {
            if (mod == null) continue;

            String id = safe(mod.id);
            String name = safe(mod.name);
            String version = safe(mod.version);
            String authors = safe(mod.authors);
            String license = resolveLicense(mod);

            rows.add(new String[]{id, name, version, authors, license});

            idW = Math.max(idW, id.length());
            nameW = Math.max(nameW, name.length());
            versionW = Math.max(versionW, version.length());
            authorsW = Math.max(authorsW, authors.length());
            licenseW = Math.max(licenseW, license.length());
        }

        String header = "| " + padRight("ID", idW)
                + " | " + padRight("Name", nameW)
                + " | " + padRight("Version", versionW)
                + " | " + padRight("Authors", authorsW)
                + " | " + padRight("License", licenseW)
                + " |";

        String sep = "|-" + repeat('-', idW)
                + "-|-" + repeat('-', nameW)
                + "-|-" + repeat('-', versionW)
                + "-|-" + repeat('-', authorsW)
                + "-|-" + repeat('-', licenseW)
                + "-|";

        LOG.info("Loaded mods (ID | Name | Version | Authors | License):");
        LOG.info(header);
        LOG.info(sep);
        for (String[] r : rows) {
            LOG.info("| {} | {} | {} | {} | {} |",
                    padRight(r[0], idW),
                    padRight(r[1], nameW),
                    padRight(r[2], versionW),
                    padRight(r[3], authorsW),
                    padRight(r[4], licenseW));
        }
    }

    private static String resolveLicense(NilMetadata mod) {
        if (mod == null || mod.source == null) return "-";
        try {
            Optional<SdkModMetadata> sdk = SdkMetadataIO.readFromSource(mod.source, mod.id);
            if (!sdk.isPresent()) return "-";
            String lic = sdk.get().getLicense();
            return isBlank(lic) ? "-" : lic.trim();
        } catch (Throwable t) {
            return "-";
        }
    }

    private static String safe(String s) {
        return isBlank(s) ? "-" : s.trim();
    }

    private static String padRight(String s, int width) {
        String v = s == null ? "" : s;
        if (v.length() >= width) return v;
        StringBuilder sb = new StringBuilder(width);
        sb.append(v);
        while (sb.length() < width) sb.append(' ');
        return sb.toString();
    }

    private static String repeat(char c, int count) {
        if (count <= 0) return "";
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) sb.append(c);
        return sb.toString();
    }

    private static int bootstrapFromDirectory(File dir, List<String> loadedOut) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return 0;

        LOG.info("Scanning directory for KDL-only candidates: {}", dir.getPath());

        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            LOG.info("No files found in {}", dir.getPath());
            return 0;
        }

        int candidates = 0;

        for (File f : files) {
            if (f == null || !f.isFile()) continue;
            String n = f.getName().toLowerCase();
            if (!n.endsWith(".jar") && !n.endsWith(".nilmod")) continue;

            candidates++;
            LOG.info("Examining archive candidate: {}", f.getPath());

            try {
                bootstrapFromArchive(f, loadedOut);
            } catch (Throwable t) {
                LOG.warn("Failed to bootstrap KDL-only mod candidate {}", f, t);
            }
        }

        LOG.info("Finished scanning {} (candidate archives={})", dir.getPath(), candidates);
        return candidates;
    }

    private static void bootstrapFromArchive(File file, List<String> loadedOut) throws Exception {
        List<String> kdlEntries = findKdlEntryNames(file);
        LOG.info("KDL metadata entries in {}: {}", file.getName(), kdlEntries);
        if (!kdlEntries.isEmpty()) {
            debugDumpFirstKdl(file, kdlEntries.get(0));
        }

        if (!isKdlOnlyArchive(file)) {
            LOG.info("Skipping {}: not a KDL-only archive", file.getName());
            return;
        }

        String id = inferModId(file);
        LOG.info("Inferred KDL-only mod id {} from {}", id, file.getName());
        if (isBlank(id)) {
            LOG.warn("Skipping {}: cannot infer mod id", file);
            return;
        }
        if (NilModList.isLoaded(id)) {
            LOG.info("Skipping {}: mod id {} is already loaded", file.getName(), id);
            return;
        }

        NilMetadata meta = createMetadata(id, file);
        if (meta == null) {
            LOG.warn("Skipping {}: failed to build metadata", file);
            return;
        }

        LOG.info("Built metadata for {}: name='{}', version='{}', authors='{}', entrypoints={}",
                id, meta.name, meta.version, meta.authors, safeEntrypoints(meta));

        Optional<SdkModMetadata> sdkMeta = SdkMetadataIO.readFromSource(file, id);
        if (sdkMeta.isPresent()) {
            SdkModMetadata sm = sdkMeta.get();
            LOG.info("SDK metadata for {}: requires={}, load_after={}, load_before={}, icon={}, modurl={}, sourceurl={}, license={}, credits={}",
                    id,
                    sm.getRequiredMods(),
                    sm.getLoadAfter(),
                    sm.getLoadBefore(),
                    sm.getIcon(),
                    sm.getModUrl(),
                    sm.getSourceUrl(),
                    sm.getLicense(),
                    sm.getCredits());
        } else {
            LOG.info("SDK metadata block not found/parsed for {}", id);
        }

        appendToSystemClasspath(file);
        LOG.info("Appended {} to system classpath/search path", file.getPath());

        injectIntoNilAgent(meta);
        LOG.info("Injected {} into NilAgent metadata/listener tables", id);

        invokePremainIfPresent(meta);
        LOG.info("Premain invocation step finished for {}", id);

        LOG.info("Bootstrapped KDL-only mod {} ({}) v{} from {}", meta.name, meta.id, meta.version, file);
        if (loadedOut != null) loadedOut.add(meta.id + "@" + meta.version);
    }

    private static void debugDumpFirstKdl(File file, String entryName) {
        JarFile jar = null;
        InputStream in = null;
        try {
            jar = new JarFile(file);
            JarEntry entry = jar.getJarEntry(entryName);
            if (entry == null) return;
            in = jar.getInputStream(entry);
            byte[] bytes = slurpBytes(in);

            StringBuilder hex = new StringBuilder();
            int n = Math.min(bytes.length, 16);
            for (int i = 0; i < n; i++) {
                if (i > 0) hex.append(' ');
                int b = bytes[i] & 0xFF;
                if (b < 0x10) hex.append('0');
                hex.append(Integer.toHexString(b).toUpperCase());
            }

            LOG.info("KDL debug {} in {}: size={} bytes, first16={} ", entryName, file.getName(), bytes.length, hex.toString());
        } catch (Throwable t) {
            LOG.warn("Failed to debug-dump KDL bytes for {} in {}", entryName, file.getName(), t);
        } finally {
            closeQuietly(in);
            closeQuietly(jar);
        }
    }

    private static byte[] slurpBytes(InputStream in) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int r;
        while ((r = in.read(buf)) >= 0) baos.write(buf, 0, r);
        return baos.toByteArray();
    }

    private static boolean isKdlOnlyArchive(File file) {
        JarFile jar = null;
        try {
            jar = new JarFile(file);
            boolean hasCss = false;
            boolean hasKdl = false;

            Enumeration<JarEntry> en = jar.entries();
            while (en.hasMoreElements()) {
                JarEntry ze = en.nextElement();
                if (ze.isDirectory()) continue;
                String name = ze.getName();
                String lower = name.toLowerCase();

                // NilLoader discovery only accepts root-level *.nilmod.css (except nilloader.nilmod.css)
                if (!name.contains("/") && lower.endsWith(".nilmod.css") && !"nilloader.nilmod.css".equals(lower)) {
                    hasCss = true;
                }
                if (lower.endsWith(KDL_SUFFIX)) {
                    hasKdl = true;
                }
                if (hasCss && hasKdl) break;
            }
            return !hasCss && hasKdl;
        } catch (Throwable t) {
            return false;
        } finally {
            closeQuietly(jar);
        }
    }

    private static String inferModId(File file) {
        List<String> candidates = findKdlEntryNames(file);
        if (!candidates.isEmpty()) {
            for (String n : candidates) {
                String base = basename(n);
                String lower = base.toLowerCase();
                if (lower.endsWith(KDL_SUFFIX) && !DEFAULT_KDL_NAME.equals(lower)) {
                    String id = sanitizeId(base.substring(0, base.length() - KDL_SUFFIX.length()));
                    if (!isBlank(id)) return id;
                }
            }
        }
        return sanitizeId(stripExtension(file.getName()));
    }

    private static List<String> findKdlEntryNames(File file) {
        JarFile jar = null;
        try {
            jar = new JarFile(file);
            List<String> out = new ArrayList<String>();
            Enumeration<JarEntry> en = jar.entries();
            while (en.hasMoreElements()) {
                JarEntry ze = en.nextElement();
                if (ze.isDirectory()) continue;
                String name = ze.getName();
                if (name.toLowerCase().endsWith(KDL_SUFFIX)) out.add(name);
            }
            return out;
        } catch (Throwable t) {
            return Collections.emptyList();
        } finally {
            closeQuietly(jar);
        }
    }

    private static NilMetadata createMetadata(String id, File source) {
        try {
            // empty CSS; bridge fills nilmod + entrypoints from KDL for missing values
            QDCSS emptyCss = QDCSS.load(id + ".nilmod.css", "");
            return NilMetadataBridge.from(id, emptyCss, source);
        } catch (Throwable t) {
            LOG.warn("Failed to create metadata for {} from {}", id, source, t);
            return null;
        }
    }

    private static void appendToSystemClasspath(File file) {
        try {
            Class<?> agent = Class.forName("nilloader.NilAgent");
            Method inject = agent.getDeclaredMethod("injectToSearchPath", JarFile.class);
            inject.setAccessible(true);

            // Keep JarFile open for JVM class-path search lifecycle (same pattern as NilAgent).
            JarFile jf = new JarFile(file);
            inject.invoke(null, jf);

            String cp = System.getProperty("java.class.path", "");
            if (!cp.contains(file.getPath())) {
                System.setProperty("java.class.path", cp + File.pathSeparator + file.getPath());
            }
        } catch (Throwable t) {
            LOG.warn("Failed to append {} to system classpath", file, t);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void injectIntoNilAgent(NilMetadata meta) throws Exception {
        Class<?> agent = Class.forName("nilloader.NilAgent");

        Field modsField = agent.getDeclaredField("mods");
        modsField.setAccessible(true);
        Map mods = (Map) modsField.get(null);
        if (mods.containsKey(meta.id)) return;
        mods.put(meta.id, meta);

        Field listenersField = agent.getDeclaredField("entrypointListeners");
        listenersField.setAccessible(true);
        Map listeners = (Map) listenersField.get(null);

        Class<?> listenerType = Class.forName("nilloader.NilAgent$EntrypointListener");
        Constructor<?> ctor = listenerType.getDeclaredConstructor(String.class, String.class);
        ctor.setAccessible(true);

        for (Map.Entry<String, String> en : safeEntrypoints(meta).entrySet()) {
            String phase = en.getKey();
            String className = en.getValue();
            if (isBlank(phase) || isBlank(className)) continue;

            // We invoke premain directly below to avoid mutating active listener iteration list.
            if ("premain".equalsIgnoreCase(phase)) continue;

            Object listener = ctor.newInstance(meta.id, className);
            List phaseList = (List) listeners.get(phase);
            if (phaseList == null) {
                phaseList = new ArrayList();
                listeners.put(phase, phaseList);
            }
            phaseList.add(listener);
        }
    }

    private static void invokePremainIfPresent(NilMetadata meta) {
        String premainClass = safeEntrypoints(meta).get("premain");
        if (isBlank(premainClass)) return;

        try {
            Class<?> agent = Class.forName("nilloader.NilAgent");
            Field activeModField = agent.getDeclaredField("activeMod");
            activeModField.setAccessible(true);

            String oldActive = (String) activeModField.get(null);
            try {
                activeModField.set(null, meta.id);

                Class<?> clazz = Class.forName(premainClass);
                Constructor<?> premainCtor = clazz.getDeclaredConstructor();
                if (!premainCtor.isAccessible()) premainCtor.setAccessible(true);
                Object o = premainCtor.newInstance();
                if (o instanceof Runnable) {
                    ((Runnable) o).run();
                } else {
                    LOG.error("Premain listener {} for {} is not Runnable", premainClass, meta.id);
                }
            } finally {
                activeModField.set(null, oldActive);
            }
        } catch (Throwable t) {
            LOG.error("Failed to invoke premain {} for {}", premainClass, meta.id, t);
        }
    }

    private static Map<String, String> safeEntrypoints(NilMetadata meta) {
        if (meta == null || meta.entrypoints == null) return Collections.emptyMap();
        return meta.entrypoints;
    }

    private static String stripExtension(String n) {
        if (n == null) return null;
        int dot = n.lastIndexOf('.');
        return dot <= 0 ? n : n.substring(0, dot);
    }

    private static String basename(String path) {
        if (path == null) return null;
        int slash = path.lastIndexOf('/');
        int backslash = path.lastIndexOf('\\');
        int idx = Math.max(slash, backslash);
        return idx < 0 ? path : path.substring(idx + 1);
    }

    private static String sanitizeId(String raw) {
        if (raw == null) return null;
        String r = raw.trim().toLowerCase();
        if (r.isEmpty()) return null;

        StringBuilder out = new StringBuilder(r.length());
        for (int i = 0; i < r.length(); i++) {
            char c = r.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.') {
                out.append(c);
            } else {
                out.append('_');
            }
        }
        return out.toString();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static void closeQuietly(java.io.Closeable c) {
        if (c == null) return;
        try {
            c.close();
        } catch (Throwable ignored) {
        }
    }
}
