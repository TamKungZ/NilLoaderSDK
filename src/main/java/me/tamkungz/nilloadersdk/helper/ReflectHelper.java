package me.tamkungz.nilloadersdk.helper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

/**
 * ReflectHelper — utility wrapper for common reflection operations.
 *
 * Reduces repeated use of getField/invoke patterns across mods.
 */
public final class ReflectHelper {

    private ReflectHelper() {}

    // ─────────────────────────────────────────────
    // FIELD ACCESS
    // ─────────────────────────────────────────────

    /**
     * Gets a field value from an object (searches superclass hierarchy).
     */
    public static Object getField(Object target, String name) throws Exception {
        Field f = findField(target.getClass(), name);
        f.setAccessible(true);
        return f.get(target);
    }

    public static int getIntField(Object target, String name) throws Exception {
        Field f = findField(target.getClass(), name);
        f.setAccessible(true);
        return f.getInt(target);
    }

    public static double getDoubleField(Object target, String name) throws Exception {
        Field f = findField(target.getClass(), name);
        f.setAccessible(true);
        return f.getDouble(target);
    }

    public static float getFloatField(Object target, String name) throws Exception {
        Field f = findField(target.getClass(), name);
        f.setAccessible(true);
        return f.getFloat(target);
    }

    public static boolean getBooleanField(Object target, String name) throws Exception {
        Field f = findField(target.getClass(), name);
        f.setAccessible(true);
        return f.getBoolean(target);
    }

    /**
     * Safe field access — returns null on error.
     */
    public static Object getFieldSafe(Object target, String name) {
        if (target == null) return null;
        try { return getField(target, name); }
        catch (Throwable ignored) { return null; }
    }

    public static int getIntFieldSafe(Object target, String name, int fallback) {
        if (target == null) return fallback;
        try { return getIntField(target, name); }
        catch (Throwable ignored) { return fallback; }
    }

    public static double getDoubleFieldSafe(Object target, String name) {
        if (target == null) return Double.NaN;
        try { return getDoubleField(target, name); }
        catch (Throwable ignored) { return Double.NaN; }
    }

    // ─────────────────────────────────────────────
    // FIELD SET
    // ─────────────────────────────────────────────

    public static void setField(Object target, String name, Object value) throws Exception {
        Field f = findField(target.getClass(), name);
        f.setAccessible(true);
        f.set(target, value);
    }

    public static void setIntField(Object target, String name, int value) throws Exception {
        Field f = findField(target.getClass(), name);
        f.setAccessible(true);
        f.setInt(target, value);
    }

    public static void setBooleanField(Object target, String name, boolean value) throws Exception {
        Field f = findField(target.getClass(), name);
        f.setAccessible(true);
        f.setBoolean(target, value);
    }

    /**
     * Attempts to set a field using multiple possible names.
     * Useful for handling obfuscated field name variations.
     */
    public static void setFieldMulti(Object target, Object value, String... names) {
        for (String name : names) {
            try { setField(target, name, value); return; }
            catch (Throwable ignored) {}
        }
    }

    public static void setIntFieldMulti(Object target, int value, String... names) {
        for (String name : names) {
            try { setIntField(target, name, value); return; }
            catch (Throwable ignored) {}
        }
    }

    // ─────────────────────────────────────────────
    // METHOD INVOCATION
    // ─────────────────────────────────────────────

    /**
     * Invokes a method on the target object using varargs.
     * Matches by method name and parameter count.
     */
    public static Object invoke(Object target, String name, Object... args) throws Exception {
        Method found = findMethodInHierarchy(target.getClass(), name, args);
        if (found == null) {
            throw new NoSuchMethodException(name + " (args=" + args.length + ") on " + target.getClass().getName());
        }
        found.setAccessible(true);

        Object[] invokeArgs = Arrays.copyOf(args, args.length);

        // Replace null with default values for primitive parameters
        for (int i = 0; i < invokeArgs.length; i++) {
            if (invokeArgs[i] == null && found.getParameterTypes()[i].isPrimitive()) {
                Class<?> pt = found.getParameterTypes()[i];
                if (pt == int.class)   invokeArgs[i] = 0;
                else if (pt == float.class)  invokeArgs[i] = 0f;
                else if (pt == double.class) invokeArgs[i] = 0.0;
                else if (pt == boolean.class) invokeArgs[i] = false;
                else if (pt == long.class) invokeArgs[i] = 0L;
                else if (pt == short.class) invokeArgs[i] = (short) 0;
                else if (pt == byte.class) invokeArgs[i] = (byte) 0;
                else if (pt == char.class) invokeArgs[i] = (char) 0;
            }
        }

        return found.invoke(target, invokeArgs);
    }

    /**
     * Safe method invocation — returns null on error.
     */
    public static Object invokeSafe(Object target, String name, Object... args) {
        if (target == null) return null;
        try { return invoke(target, name, args); }
        catch (Throwable ignored) { return null; }
    }

    // ─────────────────────────────────────────────
    // TYPE CHECKS
    // ─────────────────────────────────────────────

    /**
     * Checks if the object is an instance of any given class names
     * (including superclass hierarchy).
     *
     * Useful for obfuscated class names (e.g. "qx" = EntityPlayer).
     */
    public static boolean isInstanceOf(Object obj, String... classNames) {
        if (obj == null) return false;
        Class<?> c = obj.getClass();
        while (c != null) {
            String n = c.getName();
            for (String cn : classNames) {
                if (cn.equals(n) || n.endsWith("." + cn)) return true;
            }
            c = c.getSuperclass();
        }
        return false;
    }

    // ─────────────────────────────────────────────
    // FIELD DISCOVERY
    // ─────────────────────────────────────────────

    /**
     * Finds a field value whose type matches any of the given class name hints.
     */
    public static Object findFieldByClassHint(Object target, String... hints) {
        if (target == null || hints == null || hints.length == 0) return null;
        Class<?> c = target.getClass();
        while (c != null) {
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                try {
                    f.setAccessible(true);
                    Object v = f.get(target);
                    if (v == null) continue;
                    String cn = v.getClass().getName();
                    for (String hint : hints) {
                        if (cn.contains(hint)) return v;
                    }
                } catch (Throwable ignored) {}
            }
            c = c.getSuperclass();
        }
        return null;
    }

    /**
     * Collects field values whose type is assignable to the given class.
     */
    public static java.util.List<Object> collectFieldValuesByType(
            Object target, Class<?> assignableFrom, boolean skipStatic) {
        java.util.List<Object> results = new java.util.ArrayList<>();
        if (target == null || assignableFrom == null) return results;
        Class<?> c = target.getClass();
        while (c != null) {
            for (Field f : c.getDeclaredFields()) {
                if (skipStatic && Modifier.isStatic(f.getModifiers())) continue;
                try {
                    f.setAccessible(true);
                    Object v = f.get(target);
                    if (v != null && assignableFrom.isAssignableFrom(v.getClass())) {
                        results.add(v);
                    }
                } catch (Throwable ignored) {}
            }
            c = c.getSuperclass();
        }
        return results;
    }

    // ─────────────────────────────────────────────
    // INTERNAL
    // ─────────────────────────────────────────────

    public static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> c = type;
        while (c != null) {
            try { return c.getDeclaredField(name); }
            catch (NoSuchFieldException ignored) { c = c.getSuperclass(); }
        }
        throw new NoSuchFieldException(name + " in hierarchy of " + type.getName());
    }

    private static Method findMethodInHierarchy(Class<?> type, String name, Object[] args) {
        Class<?> c = type;
        while (c != null) {
            for (Method m : c.getDeclaredMethods()) {
                if (!m.getName().equals(name)) continue;
                Class<?>[] pts = m.getParameterTypes();
                if (pts.length != args.length) continue;
                if (!isArgsCompatible(pts, args)) continue;
                return m;
            }
            c = c.getSuperclass();
        }
        return null;
    }

    private static boolean isArgsCompatible(Class<?>[] pts, Object[] args) {
        for (int i = 0; i < pts.length; i++) {
            Object arg = args[i];
            Class<?> pt = pts[i];
            if (arg == null) {
                // invoke() intentionally supplies the Java default for primitive parameters
                // when callers pass null, so null is compatible with both reference and
                // primitive parameters here.
                continue;
            }
            Class<?> boxed = boxType(pt);
            if (!boxed.isAssignableFrom(arg.getClass())) return false;
        }
        return true;
    }

    private static Class<?> boxType(Class<?> c) {
        if (!c.isPrimitive()) return c;
        if (c == int.class) return Integer.class;
        if (c == long.class) return Long.class;
        if (c == boolean.class) return Boolean.class;
        if (c == double.class) return Double.class;
        if (c == float.class) return Float.class;
        if (c == short.class) return Short.class;
        if (c == byte.class) return Byte.class;
        if (c == char.class) return Character.class;
        return c;
    }
}