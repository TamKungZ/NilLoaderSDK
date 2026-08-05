package me.tamkungz.nilloadersdk.metadata;

import me.tamkungz.nilloadersdk.util.kdl.KdlDocument;
import me.tamkungz.nilloadersdk.util.kdl.KdlNode;
import me.tamkungz.nilloadersdk.util.kdl.KdlParser;
import me.tamkungz.nilloadersdk.util.kdl.KdlValue;
import nilloader.api.NilLogger;
import nilloader.api.NilMetadata;
import nilloader.api.lib.qdcss.QDCSS;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Runtime bridge that merges base nilmod CSS metadata with optional SDK KDL metadata.
 * CSS keeps priority, and missing values are filled from KDL.
 */
public final class NilMetadataBridge {

    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final NilLogger LOG = NilLogger.get("ModBootstrapper/MetadataBridge");

    private NilMetadataBridge() {}

    public static NilMetadata from(String id, QDCSS css, File source) {
        Map<String, String> cssEntrypoints = parseEntrypoints(css);

        String cssName = css.get("@nilmod.name").orElse(null);
        String cssDescription = css.get("@nilmod.description").orElse(null);
        String cssAuthors = css.get("@nilmod.authors").orElse(null);
        String cssVersion = css.get("@nilmod.version").orElse(null);

        KdlDoc kdl = readKdl(source, id);

        String name = firstNonBlank(cssName, kdl.nilmod.get("name"), id);
        String description = firstNonBlank(cssDescription, kdl.nilmod.get("description"), "No description provided");
        String authors = firstNonBlank(cssAuthors, kdl.nilmod.get("authors"), "No authorship provided");
        String version = firstNonBlank(cssVersion, kdl.nilmod.get("version"), "?");

        Map<String, String> outEntrypoints = new HashMap<String, String>(cssEntrypoints);
        for (Map.Entry<String, String> en : kdl.entrypoints.entrySet()) {
            String k = en.getKey();
            String v = en.getValue();
            if (!outEntrypoints.containsKey(k) || isBlank(outEntrypoints.get(k))) {
                outEntrypoints.put(k, v);
            }
        }

        return new NilMetadata(id, name, description, authors, version, Collections.unmodifiableMap(outEntrypoints), source);
    }

    private static Map<String, String> parseEntrypoints(QDCSS css) {
        Map<String, String> out = new HashMap<String, String>();
        for (Map.Entry<String, String> en : css.flatten().entrySet()) {
            if (en.getKey().startsWith("entrypoints.")) {
                out.put(en.getKey().substring(12), en.getValue());
            }
        }
        return out;
    }

    private static KdlDoc readKdl(File source, String id) {
        if (source == null || id == null || id.trim().isEmpty()) return KdlDoc.empty();
        String expected = id.trim() + ".nilsdkmod.kdl";

        try {
            if (source.isDirectory()) {
                File main = new File(source, expected);
                if (main.exists() && main.isFile()) return parseKdl(slurp(main));
                File[] all = source.listFiles();
                if (all != null) {
                    for (File f : all) {
                        if (f != null && f.isFile() && f.getName().toLowerCase().endsWith(".nilsdkmod.kdl")) {
                            return parseKdl(slurp(f));
                        }
                    }
                }
                return KdlDoc.empty();
            }

            ZipFile zip = new ZipFile(source);
            try {
                String txt = readZipEntry(zip, expected);
                if (txt == null) {
                    for (java.util.Enumeration<? extends ZipEntry> en = zip.entries(); en.hasMoreElements();) {
                        ZipEntry ze = en.nextElement();
                        if (ze.isDirectory()) continue;
                        String n = ze.getName().toLowerCase();
                        if (n.endsWith("/" + expected.toLowerCase()) || n.endsWith(".nilsdkmod.kdl")) {
                            txt = readZipEntry(zip, ze.getName());
                            if (txt != null) break;
                        }
                    }
                }
                return txt == null ? KdlDoc.empty() : parseKdl(txt);
            } finally {
                zip.close();
            }
        } catch (Throwable t) {
            return KdlDoc.empty();
        }
    }

    private static KdlDoc parseKdl(String text) {
        if (text == null || text.trim().isEmpty()) return KdlDoc.empty();
        KdlDocument document;
        try {
            document = new KdlParser(text).parse();
        } catch (Throwable t) {
            LOG.warn("Failed to parse .nilsdkmod.kdl content (length={})", text.length(), t);
            return KdlDoc.empty();
        }

        Map<String, String> nilmod = parseNilmod(document);
        Map<String, String> entrypoints = parseEntrypoints(document);
        return new KdlDoc(nilmod, entrypoints);
    }

    private static Map<String, String> parseNilmod(KdlDocument document) {
        Map<String, String> out = new LinkedHashMap<String, String>();

        List<KdlNode> sections = findSectionNodes(document, "nilmod");
        for (KdlNode section : sections) {
            readChildrenAsMap(out, section.getChildren());
            readPropertiesAsMap(out, section.getProperties());
            // Allow compact form: nilmod "Name"
            String compactName = firstArgumentString(section);
            if (!isBlank(compactName) && !out.containsKey("name")) {
                out.put("name", compactName);
            }
        }

        // Fallback: top-level nilmod keys
        for (KdlNode node : document.getNodes()) {
            String key = normalizeKey(node.getName());
            if (!isNilmodKey(key)) continue;
            String value = firstNodeValue(node);
            if (!isBlank(value) && !out.containsKey(key)) {
                out.put(key, value);
            }
        }

        return out;
    }

    private static Map<String, String> parseEntrypoints(KdlDocument document) {
        Map<String, String> out = new LinkedHashMap<String, String>();

        List<KdlNode> sections = findSectionNodes(document, "entrypoints");
        for (KdlNode section : sections) {
            readChildrenAsMap(out, section.getChildren());
            readPropertiesAsMap(out, section.getProperties());
        }

        // Fallback: top-level entrypoints.<phase> "class"
        for (KdlNode node : document.getNodes()) {
            String nodeName = node.getName();
            if (nodeName == null) continue;

            String lower = nodeName.toLowerCase();
            if (lower.startsWith("entrypoints.")) {
                String phase = nodeName.substring("entrypoints.".length()).trim();
                String value = firstNodeValue(node);
                if (!isBlank(phase) && !isBlank(value) && !out.containsKey(phase)) {
                    out.put(phase, value);
                }
            }
        }

        return out;
    }

    private static List<KdlNode> findSectionNodes(KdlDocument document, String sectionName) {
        List<KdlNode> out = new ArrayList<KdlNode>();
        if (document == null || sectionName == null) return out;

        for (KdlNode node : document.getNodes()) {
            if (node == null || node.getName() == null) continue;
            if (sectionName.equalsIgnoreCase(node.getName().trim())) {
                out.add(node);
            }
        }
        return out;
    }

    private static void readChildrenAsMap(Map<String, String> out, List<KdlNode> children) {
        if (children == null || children.isEmpty()) return;
        for (KdlNode child : children) {
            if (child == null || child.getName() == null) continue;
            String key = normalizeKey(child.getName());
            String value = firstNodeValue(child);
            if (!isBlank(key) && !isBlank(value) && !out.containsKey(key)) {
                out.put(key, value);
            }
        }
    }

    private static void readPropertiesAsMap(Map<String, String> out, Map<String, KdlValue> properties) {
        if (properties == null || properties.isEmpty()) return;
        for (Map.Entry<String, KdlValue> en : properties.entrySet()) {
            String key = normalizeKey(en.getKey());
            String value = valueToString(en.getValue());
            if (!isBlank(key) && !isBlank(value) && !out.containsKey(key)) {
                out.put(key, value.trim());
            }
        }
    }

    private static String firstNodeValue(KdlNode node) {
        if (node == null) return null;

        String fromArg = firstArgumentString(node);
        if (!isBlank(fromArg)) return fromArg;

        for (KdlValue value : node.getProperties().values()) {
            String v = valueToString(value);
            if (!isBlank(v)) return v.trim();
        }
        return null;
    }

    private static String firstArgumentString(KdlNode node) {
        if (node == null) return null;
        for (KdlValue value : node.getArguments()) {
            String v = valueToString(value);
            if (!isBlank(v)) return v.trim();
        }
        return null;
    }

    private static String valueToString(KdlValue value) {
        if (value == null) return null;
        if (value.isNull()) return "null";
        Object raw = value.getValue();
        return raw == null ? null : String.valueOf(raw);
    }

    private static boolean isNilmodKey(String key) {
        return "name".equals(key)
                || "description".equals(key)
                || "authors".equals(key)
                || "version".equals(key);
    }

    private static String normalizeKey(String key) {
        return key == null ? null : key.trim().toLowerCase();
    }

    private static String readZipEntry(ZipFile zip, String name) {
        try {
            ZipEntry ze = zip.getEntry(name);
            if (ze == null || ze.isDirectory()) return null;
            InputStream in = zip.getInputStream(ze);
            try {
                return slurp(in);
            } finally {
                in.close();
            }
        } catch (Throwable t) {
            return null;
        }
    }

    private static String slurp(File f) throws Exception {
        java.io.FileInputStream in = new java.io.FileInputStream(f);
        try {
            return slurp(in);
        } finally {
            in.close();
        }
    }

    private static String slurp(InputStream in) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int r;
        while ((r = in.read(buf)) >= 0) baos.write(buf, 0, r);
        return decodeText(baos.toByteArray());
    }

    private static String decodeText(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return "";

        // UTF-8 BOM
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xEF
                && (bytes[1] & 0xFF) == 0xBB
                && (bytes[2] & 0xFF) == 0xBF) {
            return new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        }

        // UTF-16 LE BOM
        if (bytes.length >= 2
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xFE) {
            return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16LE);
        }

        // UTF-16 BE BOM
        if (bytes.length >= 2
                && (bytes[0] & 0xFF) == 0xFE
                && (bytes[1] & 0xFF) == 0xFF) {
            return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16BE);
        }

        // UTF-16 LE (heuristic, no BOM)
        if (looksLikeUtf16Le(bytes)) {
            return new String(bytes, StandardCharsets.UTF_16LE);
        }

        // UTF-16 BE (heuristic, no BOM)
        if (looksLikeUtf16Be(bytes)) {
            return new String(bytes, StandardCharsets.UTF_16BE);
        }

        // Fallback: UTF-8
        return new String(bytes, UTF_8);
    }

    private static boolean looksLikeUtf16Le(byte[] bytes) {
        int sample = Math.min(bytes.length, 64);
        int zerosOnOdd = 0;
        int checked = 0;
        for (int i = 1; i < sample; i += 2) {
            checked++;
            if (bytes[i] == 0) zerosOnOdd++;
        }
        return checked >= 4 && zerosOnOdd >= checked - 1;
    }

    private static boolean looksLikeUtf16Be(byte[] bytes) {
        int sample = Math.min(bytes.length, 64);
        int zerosOnEven = 0;
        int checked = 0;
        for (int i = 0; i < sample; i += 2) {
            checked++;
            if (bytes[i] == 0) zerosOnEven++;
        }
        return checked >= 4 && zerosOnEven >= checked - 1;
    }

    private static String firstNonBlank(String a, String b, String fallback) {
        if (!isBlank(a)) return a;
        if (!isBlank(b)) return b;
        return fallback;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static final class KdlDoc {
        final Map<String, String> nilmod;
        final Map<String, String> entrypoints;

        KdlDoc(Map<String, String> nilmod, Map<String, String> entrypoints) {
            this.nilmod = nilmod == null ? Collections.<String, String>emptyMap() : nilmod;
            this.entrypoints = entrypoints == null ? Collections.<String, String>emptyMap() : entrypoints;
        }

        static KdlDoc empty() {
            return new KdlDoc(Collections.<String, String>emptyMap(), Collections.<String, String>emptyMap());
        }
    }
}

