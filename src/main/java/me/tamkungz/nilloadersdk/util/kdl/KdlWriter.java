package me.tamkungz.nilloadersdk.util.kdl;

import java.util.List;
import java.util.Map;

public class KdlWriter {
    private static final String INDENT = "    ";

    public String write(KdlDocument doc) {
        StringBuilder sb = new StringBuilder();
        for (KdlNode node : doc.getNodes()) {
            writeNode(sb, node, 0);
        }
        return sb.toString();
    }

    private void writeNode(StringBuilder sb, KdlNode node, int indentLevel) {
        for (int i = 0; i < indentLevel; i++) {
            sb.append(INDENT);
        }

        sb.append(escapeIdentifierIfNeeded(node.getName()));

        for (KdlValue arg : node.getArguments()) {
            sb.append(' ');
            writeValue(sb, arg);
        }

        for (Map.Entry<String, KdlValue> entry : node.getProperties().entrySet()) {
            sb.append(' ');
            sb.append(escapeIdentifierIfNeeded(entry.getKey()));
            sb.append('=');
            writeValue(sb, entry.getValue());
        }

        List<KdlNode> children = node.getChildren();
        if (!children.isEmpty()) {
            sb.append(" {\n");
            for (KdlNode child : children) {
                writeNode(sb, child, indentLevel + 1);
            }
            for (int i = 0; i < indentLevel; i++) {
                sb.append(INDENT);
            }
            sb.append("}");
        }

        sb.append("\n");
    }

    private void writeValue(StringBuilder sb, KdlValue value) {
        if (value.isString()) {
            sb.append('"');
            sb.append(escapeString(value.asString().getValue()));
            sb.append('"');
        } else if (value.isNumber() || value.isBoolean() || value.isNull()) {
            sb.append(value.toString());
        } else {
            throw new IllegalArgumentException("Unknown value type: " + value.getClass());
        }
    }

    private String escapeIdentifierIfNeeded(String id) {
        boolean needsQuote = false;
        if (id.isEmpty()) {
            needsQuote = true;
        } else {
            char first = id.charAt(0);
            if (!Character.isLetter(first) && first != '_' && first != '-') {
                needsQuote = true;
            } else {
                for (int i = 0; i < id.length(); i++) {
                    char c = id.charAt(i);
                    if (!Character.isLetterOrDigit(c) && c != '_' && c != '-') {
                        needsQuote = true;
                        break;
                    }
                }
            }
        }
        if (needsQuote || "true".equals(id) || "false".equals(id) || "null".equals(id)) {
            return "\"" + escapeString(id) + "\"";
        }
        return id;
    }

    private String escapeString(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20 || c == 0x7F) {
                        sb.append(String.format("\\u{%04X}", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}

