package me.tamkungz.nilloadersdk.util.kdl;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Small dependency-free KDL parser used by NilLoaderSDK.
 *
 * <p>The parser accepts the SDK's legacy KDL syntax and the common KDL 2.0
 * forms used by metadata files: identifier/quoted/raw strings, properties,
 * decimal/hex/octal/binary numbers, {@code #true}/{@code #false},
 * {@code #null}, keyword numbers, comments and slashdash comments.</p>
 */
public class KdlParser {
    private final String input;
    private final int length;
    private int pos;
    private int line = 1;
    private int col = 1;

    public KdlParser(String input) {
        this.input = input == null ? "" : input;
        this.length = this.input.length();
    }

    public KdlDocument parse() {
        KdlDocument doc = new KdlDocument();
        skipWhitespaceAndComments();
        while (pos < length) {
            if (startsWith("/-")) {
                advance();
                advance();
                skipWhitespaceAndComments();
                if (pos >= length) {
                    break;
                }
                parseNode(); // slashdash discards exactly one node
            } else {
                KdlNode node = parseNode();
                if (node != null) {
                    doc.addNode(node);
                }
            }
            skipWhitespaceAndComments();
        }
        return doc;
    }

    private KdlNode parseNode() {
        skipWhitespaceAndComments();
        if (pos >= length) return null;

        String name = parseStringToken();
        if (name == null) {
            throw error("Expected node name");
        }

        KdlNode node = new KdlNode(name);
        while (pos < length) {
            skipNodeSpace();
            if (pos >= length) break;

            char ch = peek();
            if (isNewline(ch) || ch == ';') {
                consumeNodeTerminator();
                break;
            }
            if (ch == '}') {
                break;
            }
            if (ch == '{') {
                parseChildren(node);
                break;
            }
            if (startsWith("/-")) {
                advance();
                advance();
                skipWhitespaceAndComments();
                if (pos >= length) break;
                if (peek() == '{') {
                    parseChildren(new KdlNode("__discarded__"));
                } else {
                    parseEntry(node, true);
                }
                continue;
            }

            parseEntry(node, false);
        }
        return node;
    }

    private void parseEntry(KdlNode node, boolean discard) {
        State before = state();
        String possibleKey = parseStringToken();
        if (possibleKey != null) {
            skipNodeSpace();
            if (pos < length && peek() == '=') {
                advance();
                skipNodeSpace();
                if (pos >= length || isNewline(peek()) || peek() == ';' || peek() == '}' || peek() == '{') {
                    throw error("Expected value for property '" + possibleKey + "'");
                }
                KdlValue value = parseValue();
                if (value == null) {
                    throw error("Expected value for property '" + possibleKey + "'");
                }
                if (!discard) {
                    node.setProperty(possibleKey, value);
                }
                return;
            }
        }

        restore(before);
        KdlValue value = parseValue();
        if (value == null) {
            throw error("Expected value");
        }
        if (!discard) {
            node.addArgument(value);
        }
    }

    private void parseChildren(KdlNode parent) {
        expect('{');
        skipWhitespaceAndComments();
        while (pos < length && peek() != '}') {
            if (startsWith("/-")) {
                advance();
                advance();
                skipWhitespaceAndComments();
                if (pos >= length) {
                    throw error("Expected node after /-");
                }
                parseNode();
            } else {
                KdlNode child = parseNode();
                if (child != null) {
                    parent.addChild(child);
                }
            }
            skipWhitespaceAndComments();
        }
        if (pos >= length || peek() != '}') {
            throw error("Expected '}'");
        }
        advance();
    }

    private KdlValue parseValue() {
        if (pos >= length) return null;

        if (startsWith("#true")) {
            consumeKeyword("#true");
            return new KdlValue.KdlBoolean(true);
        }
        if (startsWith("#false")) {
            consumeKeyword("#false");
            return new KdlValue.KdlBoolean(false);
        }
        if (startsWith("#null")) {
            consumeKeyword("#null");
            return new KdlValue.KdlNull();
        }
        if (startsWith("#-inf")) {
            consumeKeyword("#-inf");
            return new KdlValue.KdlNumber(Double.NEGATIVE_INFINITY);
        }
        if (startsWith("#inf")) {
            consumeKeyword("#inf");
            return new KdlValue.KdlNumber(Double.POSITIVE_INFINITY);
        }
        if (startsWith("#nan")) {
            consumeKeyword("#nan");
            return new KdlValue.KdlNumber(Double.NaN);
        }

        char ch = peek();
        if (ch == '"' || isRawStringStart() || isLegacyRawStringStart()) {
            return new KdlValue.KdlString(parseQuotedOrRawString());
        }
        if (looksLikeNumberStart()) {
            return parseNumber();
        }

        String ident = parseIdentifier();
        if (ident == null) return null;

        // Legacy SDK metadata used bare booleans/null before KDL 2.0 forms.
        if ("true".equals(ident)) return new KdlValue.KdlBoolean(true);
        if ("false".equals(ident)) return new KdlValue.KdlBoolean(false);
        if ("null".equals(ident)) return new KdlValue.KdlNull();
        return new KdlValue.KdlString(ident);
    }

    private String parseStringToken() {
        if (pos >= length) return null;
        char ch = peek();
        if (ch == '"' || isRawStringStart() || isLegacyRawStringStart()) {
            return parseQuotedOrRawString();
        }
        return parseIdentifier();
    }

    private String parseIdentifier() {
        if (pos >= length) return null;
        int start = pos;
        while (pos < length) {
            char c = peek();
            if (isIdentifierDelimiter(c)) break;
            advance();
        }
        if (start == pos) return null;
        return input.substring(start, pos);
    }

    private boolean isIdentifierDelimiter(char c) {
        return isWhitespace(c)
                || isNewline(c)
                || c == '(' || c == ')' || c == '{' || c == '}'
                || c == '[' || c == ']' || c == '/' || c == '\\'
                || c == '"' || c == '#' || c == ';' || c == '=';
    }

    private String parseQuotedOrRawString() {
        if (peek() == '"') {
            return parseQuotedString();
        }
        if (isRawStringStart()) {
            return parseRawString(false);
        }
        if (isLegacyRawStringStart()) {
            return parseRawString(true);
        }
        throw error("Expected string");
    }

    private String parseQuotedString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (pos < length) {
            char c = advance();
            if (c == '"') {
                return sb.toString();
            }
            if (isNewline(c)) {
                throw error("Literal newline is not allowed in a quoted string");
            }
            if (c != '\\') {
                sb.append(c);
                continue;
            }

            if (pos >= length) {
                throw error("Unfinished escape");
            }
            char e = advance();
            switch (e) {
                case 'b': sb.append('\b'); break;
                case 'f': sb.append('\f'); break;
                case 'n': sb.append('\n'); break;
                case 'r': sb.append('\r'); break;
                case 't': sb.append('\t'); break;
                case 's': sb.append(' '); break;
                case '\\': sb.append('\\'); break;
                case '"': sb.append('"'); break;
                case 'u': appendUnicodeEscape(sb); break;
                default:
                    if (isWhitespace(e) || isNewline(e)) {
                        while (pos < length && (isWhitespace(peek()) || isNewline(peek()))) {
                            advance();
                        }
                    } else {
                        throw error("Unknown escape: \\" + e);
                    }
            }
        }
        throw error("Unclosed string");
    }

    private void appendUnicodeEscape(StringBuilder sb) {
        if (pos >= length || peek() != '{') {
            throw error("Invalid unicode escape");
        }
        advance();
        int start = pos;
        while (pos < length && peek() != '}') {
            char c = peek();
            if (!isHexDigit(c) || pos - start >= 6) {
                throw error("Invalid unicode escape");
            }
            advance();
        }
        if (pos >= length || peek() != '}' || pos == start) {
            throw error("Unclosed unicode escape");
        }
        String hex = input.substring(start, pos);
        advance();
        try {
            int codePoint = Integer.parseInt(hex, 16);
            if (!Character.isValidCodePoint(codePoint)
                    || (codePoint >= Character.MIN_SURROGATE && codePoint <= Character.MAX_SURROGATE)) {
                throw error("Invalid unicode code point: " + hex);
            }
            sb.appendCodePoint(codePoint);
        } catch (NumberFormatException e) {
            throw new KdlParseException("Invalid unicode code point: " + hex + " at " + positionInfo(), e);
        }
    }

    private boolean isRawStringStart() {
        if (pos >= length || peek() != '#') return false;
        int i = pos;
        while (i < length && input.charAt(i) == '#') i++;
        return i < length && input.charAt(i) == '"';
    }

    private boolean isLegacyRawStringStart() {
        if (pos >= length || peek() != 'r') return false;
        int i = pos + 1;
        while (i < length && input.charAt(i) == '#') i++;
        return i < length && input.charAt(i) == '"';
    }

    private String parseRawString(boolean legacyRPrefix) {
        if (legacyRPrefix) {
            expect('r');
        }
        int hashes = 0;
        while (pos < length && peek() == '#') {
            hashes++;
            advance();
        }
        if (!legacyRPrefix && hashes == 0) {
            throw error("Raw string must start with '#'");
        }
        expect('"');

        StringBuilder sb = new StringBuilder();
        while (pos < length) {
            if (peek() == '"' && hasHashesAfterQuote(hashes)) {
                advance();
                for (int i = 0; i < hashes; i++) expect('#');
                return sb.toString();
            }
            char c = advance();
            if (isNewline(c)) {
                throw error("Literal newline is not allowed in a single-line raw string");
            }
            sb.append(c);
        }
        throw error("Unclosed raw string");
    }

    private boolean hasHashesAfterQuote(int hashes) {
        if (peek() != '"') return false;
        int i = pos + 1;
        for (int h = 0; h < hashes; h++) {
            if (i >= length || input.charAt(i) != '#') return false;
            i++;
        }
        return true;
    }

    private boolean looksLikeNumberStart() {
        if (pos >= length) return false;
        char c = peek();
        if (Character.isDigit(c)) return true;
        if ((c == '+' || c == '-') && pos + 1 < length) {
            return Character.isDigit(input.charAt(pos + 1));
        }
        return false;
    }

    private KdlValue parseNumber() {
        int start = pos;
        boolean negative = false;
        if (peek() == '+' || peek() == '-') {
            negative = peek() == '-';
            advance();
        }

        if (pos + 1 < length && peek() == '0') {
            char prefix = Character.toLowerCase(input.charAt(pos + 1));
            int radix = prefix == 'x' ? 16 : prefix == 'o' ? 8 : prefix == 'b' ? 2 : -1;
            if (radix != -1) {
                advance();
                advance();
                int digitsStart = pos;
                StringBuilder digits = new StringBuilder();
                while (pos < length) {
                    char c = peek();
                    if (c == '_') {
                        advance();
                        continue;
                    }
                    if (Character.digit(c, radix) < 0) break;
                    digits.append(c);
                    advance();
                }
                if (digits.length() == 0 || pos == digitsStart) {
                    throw error("Invalid radix number");
                }
                BigInteger value = new BigInteger(digits.toString(), radix);
                if (negative) value = value.negate();
                return new KdlValue.KdlNumber(value);
            }
        }

        // Rewind to include sign in decimal parsing.
        restorePositionOnly(start);
        int tokenStart = pos;
        if (peek() == '+' || peek() == '-') advance();
        int integerDigits = scanDecimalDigits();
        if (integerDigits == 0) throw error("Invalid number");

        boolean decimal = false;
        if (pos < length && peek() == '.') {
            decimal = true;
            advance();
            if (scanDecimalDigits() == 0) throw error("Expected digits after decimal point");
        }
        if (pos < length && (peek() == 'e' || peek() == 'E')) {
            decimal = true;
            advance();
            if (pos < length && (peek() == '+' || peek() == '-')) advance();
            if (scanDecimalDigits() == 0) throw error("Expected exponent digits");
        }

        String raw = input.substring(tokenStart, pos).replace("_", "");
        try {
            if (decimal) {
                return new KdlValue.KdlNumber(new BigDecimal(raw));
            }
            return new KdlValue.KdlNumber(new BigInteger(raw));
        } catch (NumberFormatException e) {
            throw new KdlParseException("Invalid number format: " + raw + " at " + positionInfo(), e);
        }
    }

    private int scanDecimalDigits() {
        int count = 0;
        while (pos < length) {
            char c = peek();
            if (Character.isDigit(c)) {
                count++;
                advance();
            } else if (c == '_') {
                advance();
            } else {
                break;
            }
        }
        return count;
    }

    private Number narrowInteger(BigInteger value) {
        if (value.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) >= 0
                && value.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) <= 0) {
            return Integer.valueOf(value.intValue());
        }
        if (value.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) >= 0
                && value.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) <= 0) {
            return Long.valueOf(value.longValue());
        }
        return value;
    }

    private void consumeKeyword(String keyword) {
        for (int i = 0; i < keyword.length(); i++) advance();
    }

    private void skipNodeSpace() {
        while (pos < length) {
            char c = peek();
            if (isWhitespace(c)) {
                advance();
                continue;
            }
            if (startsWith("//")) {
                skipLineComment();
                return;
            }
            if (startsWith("/*")) {
                skipBlockComment();
                continue;
            }
            if (c == '\\') {
                State before = state();
                advance();
                boolean saw = false;
                while (pos < length && (isWhitespace(peek()) || isNewline(peek()))) {
                    saw = true;
                    advance();
                }
                if (saw) continue;
                restore(before);
            }
            return;
        }
    }

    private void skipWhitespaceAndComments() {
        while (pos < length) {
            char c = peek();
            if (isWhitespace(c) || isNewline(c) || (pos == 0 && c == '\uFEFF')) {
                advance();
                continue;
            }
            if (startsWith("//")) {
                skipLineComment();
                continue;
            }
            if (startsWith("/*")) {
                skipBlockComment();
                continue;
            }
            return;
        }
    }

    private void skipLineComment() {
        advance();
        advance();
        while (pos < length && !isNewline(peek())) advance();
    }

    private void skipBlockComment() {
        advance();
        advance();
        int depth = 1;
        while (pos < length) {
            if (startsWith("/*")) {
                advance();
                advance();
                depth++;
            } else if (startsWith("*/")) {
                advance();
                advance();
                depth--;
                if (depth == 0) return;
            } else {
                advance();
            }
        }
        throw error("Unclosed block comment");
    }

    private void consumeNodeTerminator() {
        if (pos >= length) return;
        if (peek() == ';') {
            advance();
            return;
        }
        if (peek() == '\r') {
            advance();
            if (pos < length && peek() == '\n') advance();
            return;
        }
        if (isNewline(peek())) advance();
    }

    private boolean isWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\u00A0' || c == '\u1680'
                || (c >= '\u2000' && c <= '\u200A') || c == '\u202F'
                || c == '\u205F' || c == '\u3000';
    }

    private boolean isNewline(char c) {
        return c == '\r' || c == '\n' || c == '\u0085' || c == '\u000B'
                || c == '\u000C' || c == '\u2028' || c == '\u2029';
    }

    private boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private boolean startsWith(String s) {
        return input.startsWith(s, pos);
    }

    private char peek() {
        return input.charAt(pos);
    }

    private char advance() {
        char c = input.charAt(pos++);
        if (c == '\r') {
            line++;
            col = 1;
        } else if (c == '\n') {
            if (pos < 2 || input.charAt(pos - 2) != '\r') {
                line++;
            }
            col = 1;
        } else if (c == '\u0085' || c == '\u000B' || c == '\u000C' || c == '\u2028' || c == '\u2029') {
            line++;
            col = 1;
        } else {
            col++;
        }
        return c;
    }

    private void expect(char expected) {
        if (pos >= length || peek() != expected) {
            throw error("Expected '" + expected + "'");
        }
        advance();
    }

    private KdlParseException error(String message) {
        return new KdlParseException(message + " at " + positionInfo());
    }

    private String positionInfo() {
        return "line " + line + ", col " + col;
    }

    private State state() {
        return new State(pos, line, col);
    }

    private void restore(State state) {
        this.pos = state.pos;
        this.line = state.line;
        this.col = state.col;
    }

    /** Used only while number parsing stays on the current line. */
    private void restorePositionOnly(int newPos) {
        int delta = pos - newPos;
        pos = newPos;
        col -= delta;
        if (col < 1) col = 1;
    }

    private static final class State {
        final int pos;
        final int line;
        final int col;

        State(int pos, int line, int col) {
            this.pos = pos;
            this.line = line;
            this.col = col;
        }
    }
}
