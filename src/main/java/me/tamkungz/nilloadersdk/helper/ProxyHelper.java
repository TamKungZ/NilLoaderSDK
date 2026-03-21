package me.tamkungz.nilloadersdk.helper;

import me.tamkungz.remapping.SimpleRemap;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * ProxyHelper — injects a dynamic proxy around INetworkManager
 * on NetServerHandler.
 *
 * Used to intercept processReadPackets before the server processes packets.
 */
public final class ProxyHelper {

    private ProxyHelper() {}

    /**
     * Callback invoked before each processReadPackets call.
     */
    public interface ReadPacketInterceptor {
        /**
         * @param netMgr real INetworkManager instance
         * @param player injected EntityPlayerMP instance
         */
        void onBeforeProcessRead(Object netMgr, Object player);
    }

    /**
     * Injects a proxy around INetworkManager for the given player.
     *
     * Path: EntityPlayerMP.{nshField} → NetServerHandler.{netMgrField} → INetworkManager
     *
     * @param player      EntityPlayerMP instance
     * @param remap       SimpleRemap for resolving field names
     * @param interceptor callback triggered before processReadPackets
     * @return true if injection succeeds or proxy is already present
     */
    public static boolean injectNetworkProxy(Object player, SimpleRemap remap,
                                             ReadPacketInterceptor interceptor) {
        try {
            // EntityPlayerMP.a = playerNetServerHandler
            String nshField = remap.field("EntityPlayerMP", "playerNetServerHandler");
            Object nsh = ReflectHelper.getFieldSafe(player, nshField);
            if (nsh == null) return false;

            // NetServerHandler.b = netManager
            String netMgrField = remap.field("NetServerHandler", "netManager");
            Object netMgr = ReflectHelper.getFieldSafe(nsh, netMgrField);
            if (netMgr == null) return false;

            // Skip if already proxied
            if (Proxy.isProxyClass(netMgr.getClass())) return true;

            // Resolve INetworkManager interface
            Class<?> iNetMgr = findINetworkManagerIface(netMgr, remap);
            if (iNetMgr == null) return false;

            final Object realNetMgr = netMgr;
            final Object playerRef  = player;
            final String processReadName = remap.method("INetworkManager", "processReadPackets");

            InvocationHandler h = (proxy, method, args) -> {
                String mn = method.getName();
                boolean isRead = processReadName.equals(mn) && (args == null || args.length == 0);
                if (isRead) {
                    interceptor.onBeforeProcessRead(realNetMgr, playerRef);
                }
                try {
                    method.setAccessible(true);
                    return method.invoke(realNetMgr, args);
                } catch (java.lang.reflect.InvocationTargetException e) {
                    throw e.getCause() != null ? e.getCause() : e;
                }
            };

            Object proxy = Proxy.newProxyInstance(
                    iNetMgr.getClassLoader(), new Class<?>[]{iNetMgr}, h);

            Field nbField = ReflectHelper.findField(nsh.getClass(), netMgrField);
            nbField.setAccessible(true);
            nbField.set(nsh, proxy);
            return true;

        } catch (Throwable t) {
            return false;
        }
    }

    // ─────────────────────────────────────────────
    // INTERNAL
    // ─────────────────────────────────────────────

    private static Class<?> findINetworkManagerIface(Object netMgr, SimpleRemap remap) {
        String obfName = remap.cls("INetworkManager");
        for (Class<?> iface : netMgr.getClass().getInterfaces()) {
            String n = iface.getName();
            if (obfName.equals(n) || n.endsWith("INetworkManager")) return iface;
        }
        return null;
    }
}