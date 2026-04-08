package me.tamkungz.nilloadersdk.util.kdl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * Parser for KDL documents (v2 compatible).
 */
public class KdlParser {
    private final String input;
    private int pos = 0;
    private final int length;
    private int line = 1;
    private int col = 1;

    public KdlParser(String input) {
        this.input = input;
        this.length = input.length();
    }

    public KdlDocument parse() {
        KdlDocument doc = new KdlDocument();
        skipWhitespaceAndComments();
        while (pos < length) {
            KdlNode node = parseNode();
            if (node != null) {
                doc.addNode(node);
            }
            skipWhitespaceAndComments();
        }
        return doc;
    }

    private KdlNode parseNode() {
        skipWhitespaceAndComments();
        if (pos >= length) return null;

        // Parse node name (identifier or string)
        String name = parseIdentifierOrString();
        if (name == null) {
            throw new KdlParseException("Expected node name at " + positionInfo());
        }

        KdlNode node = new KdlNode(name);

        // Parse arguments and properties until we hit a newline, semicolon, '{', or EOF
        while (pos < length) {
            skipWhitespaceAndComments();
            if (pos >= length) break;

            char ch = peek();
            if (ch == '\n' || ch == '\r' || ch == ';') {
                // End of node (no children block)
                consumeExpected(ch);
                break;
            }
            if (ch == '{') {
                // Children block
                parseChildren(node);
                // After children block, node ends; no semicolon required
                break;
            }

            // Check for property: key=value (look ahead for '=')
            if (isIdentifierStart(ch)) {
                String key = parseIdentifier();
                skipWhitespace();
                if (pos < length && peek() == '=') {
                    pos++; col++;
                    skipWhitespace();
                    KdlValue value = parseValue();
                    if (value == null) {
                        throw new KdlParseException("Expected value for property '" + key + "' at " + positionInfo());
                    }
                    node.setProperty(key, value);
                    continue;
                } else {
                    // Not a property, treat bare identifier as string argument for compatibility.
                    node.addArgument(new KdlValue.KdlString(key));
                    continue;
                }
            }

            // Otherwise parse an argument value
            KdlValue arg = parseValue();
            if (arg == null) {
                throw new KdlParseException("Expected value at " + positionInfo());
            }
            node.addArgument(arg);
        }
        return node;
    }

    private void parseChildren(KdlNode parent) {
        consumeExpected('{');
        skipWhitespaceAndComments();
        while (pos < length && peek() != '}') {
            KdlNode child = parseNode();
            if (child != null) {
                parent.addChild(child);
            }
            skipWhitespaceAndComments();
        }
        if (pos >= length || peek() != '}') {
            throw new KdlParseException("Expected '}' at " + positionInfo());
        }
        pos++; col++;
    }

    private String parseIdentifierOrString() {
        skipWhitespaceAndComments();
        if (pos >= length) return null;

        char ch = peek();
        if (ch == '"' || (ch == 'r' && peekAheadForRawString())) {
            KdlValue val = parseString();
            if (val != null && val.isString()) {
                return val.asString().getValue();
            }
        }
        if (isIdentifierStart(ch)) {
            return parseIdentifier();
        }
        return null;
    }

    private boolean peekAheadForRawString() {
        // raw string starts with r then optional #s then "
        if (pos >= length || peek() != 'r') return false;
        int temp = pos + 1;
        while (temp < length && input.charAt(temp) == '#') temp++;
        return temp < length && input.charAt(temp) == '"';
    }

    private String parseIdentifier() {
        int start = pos;
        while (pos < length && isIdentifierPart(peek())) {
            advance();
        }
        return input.substring(start, pos);
    }

    private KdlValue parseValue() {
        skipWhitespaceAndComments();
        if (pos >= length) return null;

        char ch = peek();
        if (ch == '"' || (ch == 'r' && peekAheadForRawString())) {
            return parseString();
        }
        if (ch == '-' || ch == '+' || (ch >= '0' && ch <= '9')) {
            return parseNumber();
        }
        if (ch == 't' || ch == 'f') {
            return parseBoolean();
        }
        if (ch == 'n') {
            return parseNull();
        }
        return null;
    }

    private KdlValue parseString() {
        boolean raw = false;
        int hashCount = 0;
        if (peek() == 'r') {
            raw = true;
            advance(); // 'r'
            while (pos < length && peek() == '#') {
                hashCount++;
                advance();
            }
        }
        // expect opening quote
        if (peek() != '"') {
            throw new KdlParseException("Expected '\"' at " + positionInfo());
        }
        advance(); // consume '"'

        StringBuilder sb = new StringBuilder();
        if (raw) {
            // Raw string: find closing quote with matching hash count
            StringBuilder closingPattern = new StringBuilder("\"");
            for (int i = 0; i < hashCount; i++) closingPattern.append('#');
            String closing = closingPattern.toString();
            while (pos < length) {
                if (input.startsWith(closing, pos)) {
                    pos += closing.length();
                    col += closing.length();
                    break;
                }
                char c = advance();
                sb.append(c);
            }
        } else {
            // Regular string with escapes
            while (pos < length && peek() != '"') {
                char c = advance();
                if (c == '\\') {
                    if (pos >= length) throw new KdlParseException("Unfinished escape at " + positionInfo());
                    char ec = advance();
                    switch (ec) {
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case '\\': sb.append('\\'); break;
                        case '"': sb.append('"'); break;
                        case 'u': {
                            // parse unicode escape \\u{XXXX}
                            if (pos >= length || peek() != '{') {
                                throw new KdlParseException("Invalid unicode escape at " + positionInfo());
                            }
                            advance(); // '{'
                            int hexStart = pos;
                            while (pos < length && peek() != '}') advance();
                            if (pos >= length || peek() != '}') {
                                throw new KdlParseException("Unclosed unicode escape at " + positionInfo());
                            }
                            String hex = input.substring(hexStart, pos);
                            advance(); // '}'
                            try {
                                int codePoint = Integer.parseInt(hex, 16);
                                sb.appendCodePoint(codePoint);
                            } catch (NumberFormatException e) {
                                throw new KdlParseException("Invalid unicode code point: " + hex);
                            }
                            break;
                        }
                        default:
                            sb.append(ec);
                    }
                } else {
                    sb.append(c);
                }
            }
            if (pos >= length || peek() != '"') {
                throw new KdlParseException("Unclosed string at " + positionInfo());
            }
            advance(); // closing quote
        }
        return new KdlValue.KdlString(sb.toString());
    }

    private KdlValue parseNumber() {
        int start = pos;
        while (pos < length && (Character.isDigit(peek()) || peek() == '.' || peek() == 'e' || peek() == 'E' || peek() == '+' || peek() == '-')) {
            advance();
        }
        String numStr = input.substring(start, pos);
        try {
            if (numStr.contains(".") || numStr.contains("e") || numStr.contains("E")) {
                return new KdlValue.KdlNumber(new BigDecimal(numStr));
            } else {
                // Try to parse as integer if fits
                BigInteger bigInt = new BigInteger(numStr);
                // Try to fit into Long or Integer for convenience
                if (bigInt.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) <= 0 &&
                    bigInt.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) >= 0) {
                    long l = bigInt.longValue();
                    if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
                        return new KdlValue.KdlNumber((int) l);
                    }
                    return new KdlValue.KdlNumber(l);
                }
                return new KdlValue.KdlNumber(bigInt);
            }
        } catch (NumberFormatException e) {
            throw new KdlParseException("Invalid number format: " + numStr + " at " + positionInfo());
        }
    }

    private KdlValue parseBoolean() {
        if (input.startsWith("true", pos)) {
            pos += 4; col += 4;
            return new KdlValue.KdlBoolean(true);
        } else if (input.startsWith("false", pos)) {
            pos += 5; col += 5;
            return new KdlValue.KdlBoolean(false);
        }
        return null;
    }

    private KdlValue parseNull() {
        if (input.startsWith("null", pos)) {
            pos += 4; col += 4;
            return new KdlValue.KdlNull();
        }
        return null;
    }

    private boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_' || c == '-';
    }

    private boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '-';
    }

    private void skipWhitespace() {
        while (pos < length) {
            char c = peek();
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                advance();
            } else {
                break;
            }
        }
    }

    private void skipWhitespaceAndComments() {
        while (pos < length) {
            char c = peek();
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                advance();
                continue;
            }
            if (c == '/') {
                if (pos + 1 < length) {
                    char next = input.charAt(pos + 1);
                    if (next == '/') {
                        // line comment
                        while (pos < length && peek() != '\n') advance();
                        continue;
                    } else if (next == '*') {
                        // block comment
                        advance(); advance(); // skip /*
                        while (pos < length && !(peek() == '*' && pos + 1 < length && input.charAt(pos + 1) == '/')) {
                            advance();
                        }
                        if (pos >= length) break;
                        advance(); advance(); // skip */
                        continue;
                    } else if (next == '-') {
                        // slashdash /-
                        advance(); advance(); // skip /-
                        while (pos < length && peek() != '\n') advance();
                        continue;
                    }
                }
                break;
            }
            if (c == '\\' && pos + 1 < length && (input.charAt(pos + 1) == '\n' || input.charAt(pos + 1) == '\r')) {
                // line continuation
                advance(); // '\'
                col++;
                if (peek() == '\r') advance();
                if (peek() == '\n') advance();
                line++;
                col = 1;
                continue;
            }
            break;
        }
    }

    private char peek() {
        return input.charAt(pos);
    }

    private char advance() {
        char c = input.charAt(pos);
        pos++;
        if (c == '\n') {
            line++;
            col = 1;
        } else {
            col++;
        }
        return c;
    }

    private void consumeExpected(char expected) {
        if (pos < length && peek() == expected) {
            advance();
        }
    }

    private String positionInfo() {
        return "line " + line + ", col " + col;
    }
}
