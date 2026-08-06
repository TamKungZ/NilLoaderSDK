package me.tamkungz.nilkit.util.kdl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

public abstract class KdlValue {
    public abstract Object getValue();
    public abstract boolean isString();
    public abstract boolean isNumber();
    public abstract boolean isBoolean();
    public abstract boolean isNull();

    public KdlString asString() { throw new ClassCastException("Not a KdlString"); }
    public KdlNumber asNumber() { throw new ClassCastException("Not a KdlNumber"); }
    public KdlBoolean asBoolean() { throw new ClassCastException("Not a KdlBoolean"); }
    public KdlNull asNull() { throw new ClassCastException("Not a KdlNull"); }

    public static class KdlString extends KdlValue {
        private final String value;

        public KdlString(String value) { this.value = value; }

        @Override public String getValue() { return value; }
        @Override public boolean isString() { return true; }
        @Override public boolean isNumber() { return false; }
        @Override public boolean isBoolean() { return false; }
        @Override public boolean isNull() { return false; }
        @Override public KdlString asString() { return this; }

        @Override public String toString() { return "\"" + escape(value) + "\""; }

        private static String escape(String s) {
            return s.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
        }
    }

    public static class KdlNumber extends KdlValue {
        private final Number value;

        public KdlNumber(Number value) { this.value = value; }

        @Override public Number getValue() { return value; }
        @Override public boolean isString() { return false; }
        @Override public boolean isNumber() { return true; }
        @Override public boolean isBoolean() { return false; }
        @Override public boolean isNull() { return false; }
        @Override public KdlNumber asNumber() { return this; }

        @Override public String toString() {
            if (value instanceof Double || value instanceof Float) {
                double d = value.doubleValue();
                if (Double.isNaN(d)) return "#nan";
                if (d == Double.POSITIVE_INFINITY) return "#inf";
                if (d == Double.NEGATIVE_INFINITY) return "#-inf";
            }
            return value.toString();
        }
    }

    public static class KdlBoolean extends KdlValue {
        private final boolean value;

        public KdlBoolean(boolean value) { this.value = value; }

        @Override public Boolean getValue() { return value; }
        @Override public boolean isString() { return false; }
        @Override public boolean isNumber() { return false; }
        @Override public boolean isBoolean() { return true; }
        @Override public boolean isNull() { return false; }
        @Override public KdlBoolean asBoolean() { return this; }

        @Override public String toString() { return value ? "#true" : "#false"; }
    }

    public static class KdlNull extends KdlValue {
        @Override public Object getValue() { return null; }
        @Override public boolean isString() { return false; }
        @Override public boolean isNumber() { return false; }
        @Override public boolean isBoolean() { return false; }
        @Override public boolean isNull() { return true; }
        @Override public KdlNull asNull() { return this; }

        @Override public String toString() { return "#null"; }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KdlValue kdlValue = (KdlValue) o;
        return Objects.equals(getValue(), kdlValue.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getValue());
    }
}

