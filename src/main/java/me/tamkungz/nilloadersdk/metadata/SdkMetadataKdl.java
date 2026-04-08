package me.tamkungz.nilloadersdk.metadata;

import me.tamkungz.nilloadersdk.util.kdl.KdlDocument;
import me.tamkungz.nilloadersdk.util.kdl.KdlNode;
import me.tamkungz.nilloadersdk.util.kdl.KdlParser;
import me.tamkungz.nilloadersdk.util.kdl.KdlValue;
import me.tamkungz.nilloadersdk.util.kdl.KdlWriter;

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

        List<String> requires = new ArrayList<String>();
        List<String> before = new ArrayList<String>();
        List<String> after = new ArrayList<String>();
        String icon = null;
        String modUrl = null;
        String sourceUrl = null;
        String license = null;
        List<String> credits = new ArrayList<String>();
        boolean safeLoad = true;

        List<KdlNode> nodes = selectMetadataNodes(new KdlParser(kdlText).parse());
        for (KdlNode node : nodes) {
            String nodeName = node.getName();
            List<String> values = extractValues(node);

            if (equalsAny(nodeName, "requires", "require", "depends", "dependency")) {
                appendUnique(requires, values);
            } else if (equalsAny(nodeName, "load_before", "before", "loadbefore")) {
                appendUnique(before, values);
            } else if (equalsAny(nodeName, "load_after", "after", "loadafter")) {
                appendUnique(after, values);
            } else if (equalsAny(nodeName, "icon", "icon_path", "iconpath")) {
                if (!values.isEmpty()) icon = values.get(0);
            } else if (equalsAny(nodeName, "modurl", "mod_url", "homepage")) {
                if (!values.isEmpty()) modUrl = values.get(0);
            } else if (equalsAny(nodeName, "sourceurl", "source_url", "source")) {
                if (!values.isEmpty()) sourceUrl = values.get(0);
            } else if (equalsAny(nodeName, "license", "licence", "spdx")) {
                if (!values.isEmpty()) license = values.get(0);
            } else if (equalsAny(nodeName, "credits", "credit", "contributors", "contributor")) {
                appendUnique(credits, values);
            } else if (equalsAny(nodeName, "safeload", "safe_load", "safe")) {
                if (!values.isEmpty()) {
                    safeLoad = !"false".equalsIgnoreCase(values.get(0));
                }
            }
        }

        return new SdkModMetadata(requires, before, after, icon, modUrl, sourceUrl, license, credits, safeLoad);
    }

    public static String write(SdkModMetadata metadata) {
        SdkModMetadata m = metadata == null ? SdkModMetadata.empty() : metadata;

        KdlDocument document = new KdlDocument();
        KdlNode root = new KdlNode("nilloadersdk");

        if (!m.getRequiredMods().isEmpty()) {
            KdlNode node = new KdlNode("requires");
            addStringArgs(node, m.getRequiredMods());
            root.addChild(node);
        }
        if (!m.getLoadBefore().isEmpty()) {
            KdlNode node = new KdlNode("load_before");
            addStringArgs(node, m.getLoadBefore());
            root.addChild(node);
        }
        if (!m.getLoadAfter().isEmpty()) {
            KdlNode node = new KdlNode("load_after");
            addStringArgs(node, m.getLoadAfter());
            root.addChild(node);
        }
        if (m.getIcon() != null) {
            KdlNode node = new KdlNode("icon");
            node.addArgument(new KdlValue.KdlString(m.getIcon()));
            root.addChild(node);
        }
        if (m.getModUrl() != null) {
            KdlNode node = new KdlNode("modurl");
            node.addArgument(new KdlValue.KdlString(m.getModUrl()));
            root.addChild(node);
        }
        if (m.getSourceUrl() != null) {
            KdlNode node = new KdlNode("sourceurl");
            node.addArgument(new KdlValue.KdlString(m.getSourceUrl()));
            root.addChild(node);
        }
        if (m.getLicense() != null) {
            KdlNode node = new KdlNode("license");
            node.addArgument(new KdlValue.KdlString(m.getLicense()));
            root.addChild(node);
        }
        if (!m.getCredits().isEmpty()) {
            KdlNode node = new KdlNode("credits");
            addStringArgs(node, m.getCredits());
            root.addChild(node);
        }
        if (!m.isSafeLoad()) {
            KdlNode node = new KdlNode("safeload");
            node.addArgument(new KdlValue.KdlBoolean(false));
            root.addChild(node);
        }

        document.addNode(root);
        return new KdlWriter().write(document);
    }

    private static void addStringArgs(KdlNode node, List<String> values) {
        for (String value : values) {
            node.addArgument(new KdlValue.KdlString(value));
        }
    }

    private static List<KdlNode> selectMetadataNodes(KdlDocument document) {
        List<KdlNode> out = new ArrayList<KdlNode>();
        for (KdlNode node : document.getNodes()) {
            if ("nilloadersdk".equalsIgnoreCase(node.getName())) {
                out.addAll(node.getChildren());
                return out;
            }
        }
        out.addAll(document.getNodes());
        return out;
    }

    private static void appendUnique(List<String> target, List<String> values) {
        for (String value : values) {
            if (value == null) continue;
            String v = value.trim();
            if (!v.isEmpty() && !target.contains(v)) target.add(v);
        }
    }

    private static List<String> extractValues(KdlNode node) {
        List<String> out = new ArrayList<String>();
        for (KdlValue value : node.getArguments()) {
            String v = valueToString(value);
            if (v != null && !v.trim().isEmpty()) {
                out.add(v.trim());
            }
        }
        return out;
    }

    private static String valueToString(KdlValue value) {
        if (value == null) return null;
        if (value.isNull()) return "null";
        Object raw = value.getValue();
        return raw == null ? null : String.valueOf(raw);
    }

    private static boolean equalsAny(String value, String a, String b, String c) {
        if (value == null) return false;
        return value.equalsIgnoreCase(a) || value.equalsIgnoreCase(b) || value.equalsIgnoreCase(c);
    }

    private static boolean equalsAny(String value, String a, String b, String c, String d) {
        if (value == null) return false;
        return value.equalsIgnoreCase(a) || value.equalsIgnoreCase(b) || value.equalsIgnoreCase(c) || value.equalsIgnoreCase(d);
    }
}

