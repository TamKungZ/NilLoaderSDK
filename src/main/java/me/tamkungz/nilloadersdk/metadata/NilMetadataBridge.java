package me.tamkungz.nilloadersdk.metadata;

import nilloader.api.NilMetadata;
import nilloader.api.lib.qdcss.QDCSS;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Runtime bridge that merges base nilmod CSS metadata with optional SDK KDL metadata.
 * CSS keeps priority, and missing values are filled from KDL.
 */
public final class NilMetadataBridge {

    private static final Charset UTF_8 = Charset.forName("UTF-8");

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
        Map<String, String> nilmod = parseMapSection(text, "nilmod");
        Map<String, String> entrypoints = parseMapSection(text, "entrypoints");
        return new KdlDoc(nilmod, entrypoints);
    }

    private static Map<String, String> parseMapSection(String text, String sectionName) {
        String body = extractSectionBody(text, sectionName);
        Map<String, String> out = new LinkedHashMap<String, String>();
        if (body == null) return out;

        String[] lines = body.split("\\r?\\n");
        for (String raw : lines) {
            String line = stripLineComment(raw).trim();
            if (line.isEmpty()) continue;
            int sp = firstWhitespace(line);
            if (sp <= 0) continue;
            String key = line.substring(0, sp).trim();
            String rest = line.substring(sp + 1).trim();
            String val = firstQuoted(rest);
            if (!isBlank(key) && !isBlank(val) && !out.containsKey(key)) out.put(key, val);
        }
        return out;
    }

    private static String extractSectionBody(String text, String sectionName) {
        String lower = text.toLowerCase();
        String target = sectionName.toLowerCase();
        int idx = lower.indexOf(target);
        if (idx < 0) return null;
        int open = text.indexOf('{', idx + target.length());
        if (open < 0) return null;

        int depth = 0;
        boolean inString = false;
        for (int i = open; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' && (i == 0 || text.charAt(i - 1) != '\\')) inString = !inString;
            if (inString) continue;
            if (c == '{') depth++;
            if (c == '}') {
                depth--;
                if (depth == 0) return text.substring(open + 1, i);
            }
        }
        return null;
    }

    private static String stripLineComment(String line) {
        if (line == null || line.isEmpty()) return "";
        boolean inString = false;
        for (int i = 0; i < line.length() - 1; i++) {
            char c = line.charAt(i);
            if (c == '"' && (i == 0 || line.charAt(i - 1) != '\\')) inString = !inString;
            if (!inString && c == '/' && line.charAt(i + 1) == '/') return line.substring(0, i);
        }
        return line;
    }

    private static int firstWhitespace(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isWhitespace(s.charAt(i))) return i;
        }
        return -1;
    }

    private static String firstQuoted(String s) {
        if (s == null) return null;
        int start = s.indexOf('"');
        if (start < 0) return null;
        StringBuilder sb = new StringBuilder();
        boolean esc = false;
        for (int i = start + 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (esc) {
                sb.append(c);
                esc = false;
            } else if (c == '\\') {
                esc = true;
            } else if (c == '"') {
                return sb.toString();
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
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
        return new String(baos.toByteArray(), UTF_8);
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

