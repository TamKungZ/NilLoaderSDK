package me.tamkungz.nilloadersdk.helper;

import me.tamkungz.remapping.SimpleRemap;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * PacketHelper — utility for creating and sending Packet250CustomPayload
 * without repeatedly hardcoding logic.
 */
public final class PacketHelper {

    private PacketHelper() {}

    // ─────────────────────────────────────────────
    // BUILD PACKET250
    // ─────────────────────────────────────────────

    /**
     * Creates a Packet250CustomPayload instance.
     *
     * Tries (String, byte[]) constructor first,
     * falls back to no-arg constructor + field assignment.
     *
     * @param remap   SimpleRemap for the current version
     * @param channel channel name (e.g. "LYFE|Feed")
     * @param payload payload bytes
     * @return packet instance, or null if class cannot be found
     */
    public static Object buildPacket250(SimpleRemap remap, String channel, byte[] payload) {
        Class<?> pkt250 = findPacket250Class(remap);
        if (pkt250 == null) return null;

        // Try constructor (String, byte[])
        try {
            Object pkt = pkt250.getConstructor(String.class, byte[].class)
                    .newInstance(channel, payload);
            return pkt;
        } catch (Throwable ignored) {}

        // Fallback: no-arg constructor + set fields
        try {
            java.lang.reflect.Constructor<?> ctor = pkt250.getDeclaredConstructor();
            if (!ctor.isAccessible()) ctor.setAccessible(true);
            Object pkt = ctor.newInstance();
            String chField   = remap.field("Packet250CustomPayload", "channel");
            String dataField = remap.field("Packet250CustomPayload", "data");
            String lenField  = remap.field("Packet250CustomPayload", "length");

            ReflectHelper.setFieldMulti(pkt, channel,  chField, "channel", "a");
            ReflectHelper.setFieldMulti(pkt, payload,  dataField, "data", "c");
            ReflectHelper.setIntFieldMulti(pkt, payload.length, lenField, "length", "b");

            return pkt;
        } catch (Throwable ignored) {}

        return null;
    }

    // ─────────────────────────────────────────────
    // SEND PACKET
    // ─────────────────────────────────────────────

    /**
     * Sends a packet using best-effort strategy.
     *
     * Scans multiple possible paths (NetClientHandler, player fields, etc.).
     *
     * @return true if sending succeeds
     */
    public static boolean sendPacketBestEffort(Object mc, Object clientPlayer,
                                               Object pkt, SimpleRemap remap) {
        if (pkt == null) return false;
        Class<?> pktClass = pkt.getClass();

        Object[] candidates = new Object[]{
                getNetClientHandlerSafe(mc, remap),
                ReflectHelper.getFieldSafe(clientPlayer, "bc"),
                ReflectHelper.getFieldSafe(clientPlayer, "a"),
                ReflectHelper.findFieldByClassHint(clientPlayer, "NetClientHandler", "NetClient"),
                ReflectHelper.getFieldSafe(mc, remap.field("Minecraft", "netClientHandler")),
                ReflectHelper.findFieldByClassHint(mc, "NetClientHandler", "NetClient")
        };

        for (Object c : candidates) {
            if (tryInvokeSend(c, pkt, pktClass)) return true;
        }

        if (scanFieldsForSend(clientPlayer, pkt, pktClass)) return true;
        return scanFieldsForSend(mc, pkt, pktClass);
    }

    // ─────────────────────────────────────────────
    // PACKET INSPECTION
    // ─────────────────────────────────────────────

    /**
     * Extracts the channel name from a Packet250 instance.
     */
    public static String getChannel(Object pkt250, SimpleRemap remap) {
        String fieldName = remap.field("Packet250CustomPayload", "channel");
        Object v = ReflectHelper.getFieldSafe(pkt250, fieldName);
        if (v instanceof String) return (String) v;

        v = ReflectHelper.getFieldSafe(pkt250, "channel");
        return v instanceof String ? (String) v : null;
    }

    /**
     * Extracts payload bytes from a Packet250 instance.
     */
    public static byte[] getData(Object pkt250, SimpleRemap remap) {
        String fieldName = remap.field("Packet250CustomPayload", "data");
        Object v = ReflectHelper.getFieldSafe(pkt250, fieldName);
        if (v instanceof byte[]) return (byte[]) v;

        v = ReflectHelper.getFieldSafe(pkt250, "data");
        return v instanceof byte[] ? (byte[]) v : null;
    }

    /**
     * Checks whether the object is a Packet250 for the given channel.
     */
    public static boolean isPacket250ForChannel(Object obj, String channel, SimpleRemap remap) {
        if (obj == null) return false;
        String cn = obj.getClass().getName();
        if (!"di".equals(cn) && !cn.endsWith("Packet250CustomPayload")) return false;
        return channel.equals(getChannel(obj, remap));
    }

    // ─────────────────────────────────────────────
    // INTERNAL
    // ─────────────────────────────────────────────

    private static Object getNetClientHandlerSafe(Object mc, SimpleRemap remap) {
        if (mc == null) return null;
        String mName = remap.method("Minecraft", "getNetClientHandler");
        try {
            for (Method m : mc.getClass().getMethods()) {
                if (!mName.equals(m.getName()) || m.getParameterTypes().length != 0) continue;
                Class<?> rt = m.getReturnType();
                if (rt == Void.TYPE || rt.isPrimitive()) continue;
                String rcn = rt.getName();
                if (rcn.contains("NetClientHandler") || rcn.equals("ayh")) {
                    m.setAccessible(true);
                    return m.invoke(mc);
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static boolean tryInvokeSend(Object target, Object pkt, Class<?> pktClass) {
        if (target == null) return false;
        for (Method m : target.getClass().getMethods()) {
            if (m.getParameterTypes().length != 1) continue;
            if (!m.getParameterTypes()[0].isAssignableFrom(pktClass)) continue;
            String mn = m.getName();
            if (!"addToSendQueue".equals(mn) && !"func_72497_a".equals(mn)
                    && !"a".equals(mn) && !"b".equals(mn) && !"c".equals(mn)) continue;
            try {
                m.setAccessible(true);
                m.invoke(target, pkt);
                return true;
            } catch (Throwable ignored) {}
        }
        return false;
    }

    private static boolean scanFieldsForSend(Object root, Object pkt, Class<?> pktClass) {
        if (root == null) return false;
        Class<?> c = root.getClass();
        while (c != null) {
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                try {
                    f.setAccessible(true);
                    Object v = f.get(root);
                    if (v == null || v == root) continue;
                    if (tryInvokeSend(v, pkt, pktClass)) return true;
                } catch (Throwable ignored) {}
            }
            c = c.getSuperclass();
        }
        return false;
    }

    private static Class<?> findPacket250Class(SimpleRemap remap) {
        String[] names = {
                remap.cls("Packet250CustomPayload"),
                "net.minecraft.network.packet.Packet250CustomPayload",
                "net.minecraft.src.Packet250CustomPayload",
                "Packet250CustomPayload",
                "di"
        };
        for (String n : names) {
            try { return Class.forName(n); }
            catch (Throwable ignored) {}
        }
        return null;
    }
}