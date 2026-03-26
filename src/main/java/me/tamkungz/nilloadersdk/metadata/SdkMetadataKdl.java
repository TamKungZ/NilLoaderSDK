package me.tamkungz.nilloadersdk.metadata;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal KDL codec for SDK metadata.
 *
 * <p>Supported schema:</p>
 * <pre>
 * nilloadersdk {
 *   requires "modA" "modB"
 *   load_before "other_mod"
 *   load_after "another_mod"
 *   icon "assets/example/icon.png"
 * }
 * </pre>
 */
public final class SdkMetadataKdl {

    private SdkMetadataKdl() {}

    public static SdkModMetadata parse(String kdlText) {
        if (kdlText == null || kdlText.trim().isEmpty()) return SdkModMetadata.empty();

        String body = extractRootBody(kdlText, "nilloadersdk");
        if (body == null) body = kdlText;

        List<String> requires = new ArrayList<String>();
        List<String> before = new ArrayList<String>();
        List<String> after = new ArrayList<String>();
        String icon = null;
        boolean safeLoad = true;

        String[] lines = body.split("\\r?\\n");
        for (String raw : lines) {
            String line = stripComments(raw).trim();
            if (line.isEmpty() || line.equals("{") || line.equals("}")) continue;

            if (line.endsWith("{") || line.endsWith("}")) {
                line = line.substring(0, line.length() - 1).trim();
            }
            if (line.isEmpty()) continue;

            int sp = findFirstWhitespace(line);
            String nodeName = sp < 0 ? line : line.substring(0, sp);
            String args = sp < 0 ? "" : line.substring(sp + 1).trim();
            List<String> values = extractValues(args);

            if (equalsAny(nodeName, "requires", "require", "depends", "dependency")) {
                appendUnique(requires, values);
            } else if (equalsAny(nodeName, "load_before", "before", "loadbefore")) {
                appendUnique(before, values);
            } else if (equalsAny(nodeName, "load_after", "after", "loadafter")) {
                appendUnique(after, values);
            } else if (equalsAny(nodeName, "icon", "icon_path", "iconpath")) {
                if (!values.isEmpty()) icon = values.get(0);
            } else if (equalsAny(nodeName, "safeload", "safe_load", "safe")) {
                if (!values.isEmpty()) {
                    safeLoad = !"false".equalsIgnoreCase(values.get(0));
                }
            }
        }

        return new SdkModMetadata(requires, before, after, icon, safeLoad);
    }

    public static String write(SdkModMetadata metadata) {
        SdkModMetadata m = metadata == null ? SdkModMetadata.empty() : metadata;

        StringBuilder sb = new StringBuilder();
        sb.append("nilloadersdk {\n");

        if (!m.getRequiredMods().isEmpty()) {
            sb.append("  requires");
            appendQuotedList(sb, m.getRequiredMods());
            sb.append('\n');
        }
        if (!m.getLoadBefore().isEmpty()) {
            sb.append("  load_before");
            appendQuotedList(sb, m.getLoadBefore());
            sb.append('\n');
        }
        if (!m.getLoadAfter().isEmpty()) {
            sb.append("  load_after");
            appendQuotedList(sb, m.getLoadAfter());
            sb.append('\n');
        }
        if (m.getIcon() != null) {
            sb.append("  icon \"").append(escape(m.getIcon())).append("\"\n");
        }
        if (!m.isSafeLoad()) {
            sb.append("  safeload false\n");
        }

        sb.append("}\n");
        return sb.toString();
    }

    private static void appendQuotedList(StringBuilder sb, List<String> values) {
        for (String value : values) {
            sb.append(' ').append('"').append(escape(value)).append('"');
        }
    }

    private static void appendUnique(List<String> target, List<String> values) {
        for (String value : values) {
            if (value == null) continue;
            String v = value.trim();
            if (!v.isEmpty() && !target.contains(v)) target.add(v);
        }
    }

    private static boolean equalsAny(String value, String a, String b, String c) {
        if (value == null) return false;
        return value.equalsIgnoreCase(a) || value.equalsIgnoreCase(b) || value.equalsIgnoreCase(c);
    }

    private static boolean equalsAny(String value, String a, String b, String c, String d) {
        if (value == null) return false;
        return value.equalsIgnoreCase(a) || value.equalsIgnoreCase(b) || value.equalsIgnoreCase(c) || value.equalsIgnoreCase(d);
    }

    private static int findFirstWhitespace(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isWhitespace(s.charAt(i))) return i;
        }
        return -1;
    }

    private static String stripComments(String in) {
        if (in == null || in.isEmpty()) return "";

        boolean inString = false;
        for (int i = 0; i < in.length() - 1; i++) {
            char c = in.charAt(i);
            if (c == '"' && (i == 0 || in.charAt(i - 1) != '\\')) inString = !inString;
            if (!inString && c == '/' && in.charAt(i + 1) == '/') {
                return in.substring(0, i);
            }
        }
        return in;
    }

    private static List<String> extractValues(String args) {
        List<String> out = new ArrayList<String>();
        if (args == null || args.isEmpty()) return out;

        int i = 0;
        while (i < args.length()) {
            while (i < args.length() && Character.isWhitespace(args.charAt(i))) i++;
            if (i >= args.length()) break;

            char c = args.charAt(i);
            if (c == '"') {
                i++;
                StringBuilder sb = new StringBuilder();
                boolean escaping = false;
                while (i < args.length()) {
                    char cc = args.charAt(i++);
                    if (escaping) {
                        sb.append(cc);
                        escaping = false;
                    } else if (cc == '\\') {
                        escaping = true;
                    } else if (cc == '"') {
                        break;
                    } else {
                        sb.append(cc);
                    }
                }
                String v = sb.toString().trim();
                if (!v.isEmpty()) out.add(v);
            } else {
                int start = i;
                while (i < args.length() && !Character.isWhitespace(args.charAt(i))) i++;
                String v = args.substring(start, i).trim();
                if (!v.isEmpty()) out.add(v);
            }
        }

        return out;
    }

    private static String extractRootBody(String text, String rootName) {
        String lower = text.toLowerCase();
        String needle = rootName.toLowerCase();
        int idx = lower.indexOf(needle);
        if (idx < 0) return null;

        int braceOpen = text.indexOf('{', idx + needle.length());
        if (braceOpen < 0) return null;

        int depth = 0;
        boolean inString = false;
        for (int i = braceOpen; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' && (i == 0 || text.charAt(i - 1) != '\\')) inString = !inString;
            if (inString) continue;

            if (c == '{') depth++;
            if (c == '}') {
                depth--;
                if (depth == 0) {
                    return text.substring(braceOpen + 1, i);
                }
            }
        }
        return null;
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

