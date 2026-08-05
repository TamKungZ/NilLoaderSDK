package me.tamkungz.nilloadersdk.helper;

import me.tamkungz.remapping.SimpleRemap;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * McHelper — utility methods for interacting with Minecraft instance,
 * players, and world data.
 *
 * Works with SimpleRemap to simplify obfuscated access.
 */
public final class McHelper {

    private McHelper() {}

    // ─────────────────────────────────────────────
    // MINECRAFT INSTANCE
    // ─────────────────────────────────────────────

    /**
     * Returns the Minecraft instance without linking NilLoaderSDK to a specific
     * Minecraft class at class-load time. The target class is resolved only when
     * this helper is explicitly called.
     */
    public static Object getMinecraft(SimpleRemap remap) throws Exception {
        if (remap == null) throw new IllegalArgumentException("remap must not be null");

        Class<?> minecraftClass = loadClass(remap.cls("Minecraft"));
        String getter = remap.method("Minecraft", "getInstance");

        try {
            java.lang.reflect.Method method = minecraftClass.getMethod(getter);
            if (!method.isAccessible()) method.setAccessible(true);
            return method.invoke(null);
        } catch (NoSuchMethodException ignored) {
            java.lang.reflect.Method method = minecraftClass.getDeclaredMethod(getter);
            if (!method.isAccessible()) method.setAccessible(true);
            return method.invoke(null);
        }
    }

    /** Safe version of getMinecraft (returns null on failure). */
    public static Object getMinecraftSafe(SimpleRemap remap) {
        try { return getMinecraft(remap); }
        catch (Throwable ignored) { return null; }
    }

    /** Returns true when the mapped Minecraft client class can be resolved. */
    public static boolean isMinecraftPresent(SimpleRemap remap) {
        if (remap == null) return false;
        try {
            loadClass(remap.cls("Minecraft"));
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    // ─────────────────────────────────────────────
    // PLAYER
    // ─────────────────────────────────────────────

    /** Returns the local player instance. */
    public static Object getLocalPlayer(Object mc, SimpleRemap remap) {
        return ReflectHelper.getFieldSafe(mc, remap.field("Minecraft", "thePlayer"));
    }

    /** Returns the player name using multiple fallback methods. */
    public static String getPlayerName(Object player, SimpleRemap remap) {
        for (String m : new String[]{
                remap.method("EntityPlayer", "getUsername"), "an", "c_"
        }) {
            Object v = ReflectHelper.invokeSafe(player, m);
            if (v != null) return String.valueOf(v);
        }

        Object v = ReflectHelper.getFieldSafe(player, remap.field("EntityPlayer", "username"));
        if (v != null) return String.valueOf(v);
        return null;
    }

    /** Returns true if the player is in creative mode. */
    public static boolean isCreative(Object player, SimpleRemap remap) {
        Object r = ReflectHelper.invokeSafe(player, remap.method("EntityPlayer", "isCreativeMode"));
        return r instanceof Boolean && (Boolean) r;
    }

    /** Returns true if the player is not in creative mode. */
    public static boolean isSurvivalLike(Object player, SimpleRemap remap) {
        return !isCreative(player, remap);
    }

    // ─────────────────────────────────────────────
    // HELD ITEM
    // ─────────────────────────────────────────────

    public static Object getHeldItemStack(Object player, SimpleRemap remap) {
        return ReflectHelper.invokeSafe(player, remap.method("EntityPlayer", "getHeldItem"));
    }

    public static Object getItemFromStack(Object stack, SimpleRemap remap) {
        if (stack == null) return null;
        return ReflectHelper.invokeSafe(stack, remap.method("ItemStack", "getItem"));
    }

    public static int getItemId(Object item, SimpleRemap remap) {
        return ReflectHelper.getIntFieldSafe(item, remap.field("Item", "itemID"), -1);
    }

    public static int getHeldItemId(Object player, SimpleRemap remap) {
        Object stack = getHeldItemStack(player, remap);
        if (stack == null) return -1;
        Object item = getItemFromStack(stack, remap);
        if (item == null) return -1;
        return getItemId(item, remap);
    }

    // ─────────────────────────────────────────────
    // FOOD STATS
    // ─────────────────────────────────────────────

    /** Returns FoodStats instance with fallbacks. */
    public static Object getFoodStats(Object player, SimpleRemap remap) {
        Object fs = ReflectHelper.invokeSafe(player, remap.method("EntityPlayer", "getFoodStats"));
        if (fs != null) return fs;

        for (String f : new String[]{
                remap.field("EntityPlayer", "foodStats"), "bM", "bN", "bL", "foodStats"
        }) {
            Object v = ReflectHelper.getFieldSafe(player, f);
            if (v != null && !(v instanceof Boolean) && !(v instanceof Integer)) return v;
        }
        return null;
    }

    /** Adds food stats to the player. */
    public static void addFoodStats(Object player, int hunger, float saturation, SimpleRemap remap) {
        Object fs = getFoodStats(player, remap);
        if (fs == null) return;
        ReflectHelper.invokeSafe(fs, remap.method("FoodStats", "addStats"),
                Integer.valueOf(hunger), Float.valueOf(saturation));
    }

    // ─────────────────────────────────────────────
    // INVENTORY
    // ─────────────────────────────────────────────

    public static int getStackSize(Object stack, SimpleRemap remap) {
        return ReflectHelper.getIntFieldSafe(stack, remap.field("ItemStack", "stackSize"), 0);
    }

    public static void setStackSize(Object stack, int size, SimpleRemap remap) throws Exception {
        ReflectHelper.setIntField(stack, remap.field("ItemStack", "stackSize"), size);
    }

    /** Writes the held item stack into the player's inventory. */
    public static void writeHeldStack(Object player, Object stack, SimpleRemap remap) {
        try {
            String invField = remap.field("EntityPlayer", "inventory");
            Object inv = ReflectHelper.getField(player, invField);
            if (inv == null) return;

            int slot = ReflectHelper.getIntField(inv, remap.field("InventoryPlayer", "currentItem"));
            Object[] main = (Object[]) ReflectHelper.getField(inv, remap.field("InventoryPlayer", "mainInventory"));

            if (main != null && slot >= 0 && slot < main.length) {
                main[slot] = stack;
            }

            ReflectHelper.setBooleanField(inv, remap.field("InventoryPlayer", "inventoryChanged"), true);
        } catch (Throwable ignored) {}
    }

    public static void clearHeldStack(Object player, SimpleRemap remap) {
        writeHeldStack(player, null, remap);
        ReflectHelper.invokeSafe(player, remap.method("EntityPlayer", "swingItem"));
    }

    // ─────────────────────────────────────────────
    // INTEGRATED SERVER
    // ─────────────────────────────────────────────

    /** Returns integrated server instance if present. */
    public static Object getIntegratedServer(Object mc) {
        for (String f : new String[]{"t", "s", "u", "integratedServer"}) {
            Object v = ReflectHelper.getFieldSafe(mc, f);
            if (v != null) {
                String cn = v.getClass().getName();
                if (cn.contains("IntegratedServer") || cn.contains("MinecraftServer")) return v;
            }
        }
        return null;
    }

    public static boolean isHost(Object mc) {
        return getIntegratedServer(mc) != null;
    }

    public static Object getServerConfigManager(Object mc, SimpleRemap remap) {
        Object server = getIntegratedServer(mc);
        if (server == null) return null;
        return ReflectHelper.invokeSafe(server,
                remap.method("MinecraftServer", "getConfigurationManager"));
    }

    @SuppressWarnings("unchecked")
    public static List<Object> getServerPlayerList(Object mc, SimpleRemap remap) {
        Object scm = getServerConfigManager(mc, remap);
        if (scm == null) return new ArrayList<>();

        Object list = ReflectHelper.getFieldSafe(scm,
                remap.field("ServerConfigurationManager", "playerList"));

        return (list instanceof List) ? (List<Object>) list : new ArrayList<>();
    }

    public static Object getServerPlayerByName(Object mc, String name, SimpleRemap remap) {
        Object scm = getServerConfigManager(mc, remap);
        if (scm == null) return null;

        Object p = ReflectHelper.invokeSafe(scm,
                remap.method("ServerConfigurationManager", "getPlayerByUsername"), name);
        if (p != null) return p;

        for (Object player : getServerPlayerList(mc, remap)) {
            if (player == null) continue;
            String n = getPlayerName(player, remap);
            if (name.equalsIgnoreCase(n)) return player;
        }
        return null;
    }

    // ─────────────────────────────────────────────
    // POSITION & LOOK
    // ─────────────────────────────────────────────

    /** Returns entity position as [x, y, z], or null if unavailable. */
    public static double[] getEntityPos(Object entity) {
        double x = tryDouble(entity, "t", "u", "s", "p", "x");
        double y = tryDouble(entity, "u", "v", "t", "q", "y");
        double z = tryDouble(entity, "v", "w", "u", "r", "z");

        if (!Double.isNaN(x) && !Double.isNaN(y) && !Double.isNaN(z))
            return new double[]{x, y, z};

        return null;
    }

    /** Returns normalized look vector. */
    public static double[] getLookVec(Object entity, SimpleRemap remap) {
        try {
            Object look = ReflectHelper.invoke(entity, remap.method("EntityPlayer", "getLookVec"));
            if (look != null) return extractVec3(look, remap);
        } catch (Throwable ignored) {}

        try {
            Object look = ReflectHelper.invoke(entity, "i", Float.valueOf(1.0f));
            if (look != null) return extractVec3(look, remap);
        } catch (Throwable ignored) {}

        return null;
    }

    public static double[] extractVec3(Object vec, SimpleRemap remap) throws Exception {
        double x = ReflectHelper.getDoubleField(vec, remap.field("Vec3", "xCoord"));
        double y = ReflectHelper.getDoubleField(vec, remap.field("Vec3", "yCoord"));
        double z = ReflectHelper.getDoubleField(vec, remap.field("Vec3", "zCoord"));

        double mag = Math.sqrt(x * x + y * y + z * z);
        if (mag > 1e-6) { x /= mag; y /= mag; z /= mag; }

        return new double[]{x, y, z};
    }

    // ─────────────────────────────────────────────
    // WORLD / ENTITY COLLECTION
    // ─────────────────────────────────────────────

    /** Returns world object from an entity. */
    public static Object getWorldFromEntity(Object entity, SimpleRemap remap) {
        Object world = ReflectHelper.getFieldSafe(entity,
                remap.field("EntityPlayer", "worldObj"));
        if (world != null) return world;

        return ReflectHelper.findFieldByClassHint(entity,
                remap.cls("World"), remap.cls("WorldClient"), "yc", "ayp");
    }

    /** Collects all player-like entities in the world (excluding given object). */
    public static List<Object> collectPlayers(Object world, Object exclude, SimpleRemap remap) {
        List<Object> out = new ArrayList<>();
        String entityPlayerClass = remap.cls("EntityPlayer");

        Class<?> wc = world.getClass();
        while (wc != null) {
            for (Field f : wc.getDeclaredFields()) {
                if (!List.class.isAssignableFrom(f.getType())) continue;
                if (Modifier.isStatic(f.getModifiers())) continue;

                try {
                    f.setAccessible(true);
                    Object v = f.get(world);
                    if (!(v instanceof List)) continue;

                    for (Object obj : (List<?>) v) {
                        if (obj == null || obj == exclude) continue;
                        if (ReflectHelper.isInstanceOf(obj, entityPlayerClass, "qx")) {
                            if (!out.contains(obj)) out.add(obj);
                        }
                    }
                } catch (Throwable ignored) {}
            }
            wc = wc.getSuperclass();
        }

        return out;
    }

    // ─────────────────────────────────────────────
    // SOUND
    // ─────────────────────────────────────────────

    public static void playSound(Object player, String sound, float volume, float pitch, SimpleRemap remap) {
        ReflectHelper.invokeSafe(player, remap.method("EntityPlayer", "playSound"),
                sound, Float.valueOf(volume), Float.valueOf(pitch));
    }

    // ─────────────────────────────────────────────
    // INTERNAL
    // ─────────────────────────────────────────────

    private static Class<?> loadClass(String name) throws ClassNotFoundException {
        if (name == null || name.trim().isEmpty()) {
            throw new ClassNotFoundException("Minecraft class mapping is blank");
        }

        String binaryName = name.replace('/', '.');
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        if (context != null) {
            try {
                return Class.forName(binaryName, false, context);
            } catch (ClassNotFoundException ignored) {
                // Fall through to the SDK's own loader.
            }
        }

        ClassLoader own = McHelper.class.getClassLoader();
        if (own != null) {
            return Class.forName(binaryName, false, own);
        }
        return Class.forName(binaryName);
    }

    private static double tryDouble(Object target, String... names) {
        for (String n : names) {
            double v = ReflectHelper.getDoubleFieldSafe(target, n);
            if (!Double.isNaN(v)) return v;
        }
        return Double.NaN;
    }
}