package me.tamkungz.remapping;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * SimpleRemap — mapping table between human-readable names and obfuscated names.
 *
 * Avoids hardcoding values like "bD", "cj", "iq" across the codebase.
 *
 * Default mappings are for Minecraft Beta 1.7.3 (used in NilLoaderSDK examples).
 *
 * Usage:
 *   SimpleRemap remap = SimpleRemap.forVersion("1.4.7");
 *   String fieldName = remap.field("EntityPlayer", "inventory");     // → "bJ"
 *   String methodName = remap.method("EntityPlayer", "getHeldItem"); // → "bD"
 */
public final class SimpleRemap {

    private final String version;
    private final Map<String, String> fields  = new HashMap<>();
    private final Map<String, String> methods = new HashMap<>();
    private final Map<String, String> classes = new HashMap<>();

    private SimpleRemap(String version) {
        this.version = version;
    }

    // ─────────────────────────────────────────────
    // FACTORY
    // ─────────────────────────────────────────────

    public static SimpleRemap forVersion(String version) {
        if ("1.4.7".equals(version)) {
            SimpleRemap r = build147();
            loadFromSrgIfPresent(r, version);
            return r;
        }
        if ("1.6.2".equals(version)) {
            SimpleRemap r = new SimpleRemap("1.6.2");
            if (loadFromSrgIfPresent(r, version)) {
                return r;
            }
            return r;
        }
        throw new IllegalArgumentException("No built-in mappings for version: " + version);
    }

    /** Creates an empty remap for custom or unsupported versions. */
    public static SimpleRemap empty(String version) {
        return new SimpleRemap(version);
    }

    // ─────────────────────────────────────────────
    // LOOKUP
    // ─────────────────────────────────────────────

    /**
     * Returns obfuscated field name.
     * Key format: "ClassName.fieldName" (e.g. "EntityPlayer.inventory").
     * Falls back to the original name if not found.
     */
    public String field(String className, String friendlyName) {
        String key = className + "." + friendlyName;
        return fields.getOrDefault(key, friendlyName);
    }

    /** Returns obfuscated method name. */
    public String method(String className, String friendlyName) {
        String key = className + "." + friendlyName;
        return methods.getOrDefault(key, friendlyName);
    }

    /** Returns obfuscated class name. */
    public String cls(String friendlyName) {
        return classes.getOrDefault(friendlyName, friendlyName);
    }

    public String getVersion() { return version; }

    private static boolean loadFromSrgIfPresent(SimpleRemap r, String version) {
        // 1) Load from bundled resource (inside JAR)
        String resourcePath = "/remapping/" + version + "/mcp2obf.srg";
        try (InputStream in = SimpleRemap.class.getResourceAsStream(resourcePath)) {
            if (in != null) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    parseSrg(br, r);
                    return true;
                }
            }
        } catch (Exception ignored) {}

        // 2) Fallback for local development environment
        File srg = new File(".remapping/" + version + "/mcp2obf.srg");
        if (!srg.exists()) return false;

        try (BufferedReader br = new BufferedReader(new FileReader(srg))) {
            parseSrg(br, r);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static void parseSrg(BufferedReader br, SimpleRemap r) throws Exception {
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            if (line.startsWith("CL: ")) {
                String[] p = line.substring(4).trim().split("\\s+");
                if (p.length >= 2) {
                    String friendly = simpleClass(p[0]);
                    String obf = normalizeClass(p[1]);
                    r.addClass(friendly, obf);
                }
            } else if (line.startsWith("FD: ")) {
                String[] p = line.substring(4).trim().split("\\s+");
                if (p.length >= 2) {
                    String[] left = splitOwnerName(p[0]);
                    String[] right = splitOwnerName(p[1]);
                    if (left != null && right != null) {
                        r.addField(simpleClass(left[0]), left[1], right[1]);
                    }
                }
            } else if (line.startsWith("MD: ")) {
                String[] p = line.substring(4).trim().split("\\s+");
                if (p.length >= 4) {
                    String[] left = splitOwnerName(p[0]);
                    String[] right = splitOwnerName(p[2]);
                    if (left != null && right != null) {
                        r.addMethod(simpleClass(left[0]), left[1], right[1]);
                    }
                }
            }
        }
    }

    private static String normalizeClass(String internalName) {
        if (internalName == null) return null;
        if (internalName.indexOf('/') >= 0) return internalName.replace('/', '.');
        return internalName;
    }

    private static String simpleClass(String internalName) {
        if (internalName == null) return null;
        int idx = internalName.lastIndexOf('/');
        return idx >= 0 ? internalName.substring(idx + 1) : internalName;
    }

    private static String[] splitOwnerName(String ownerAndName) {
        int idx = ownerAndName.lastIndexOf('/');
        if (idx <= 0 || idx >= ownerAndName.length() - 1) return null;
        return new String[] {
                ownerAndName.substring(0, idx),
                ownerAndName.substring(idx + 1)
        };
    }

    // ─────────────────────────────────────────────
    // BUILDER HELPERS
    // ─────────────────────────────────────────────

    public SimpleRemap addField(String className, String friendly, String obf) {
        fields.put(className + "." + friendly, obf);
        return this;
    }

    public SimpleRemap addMethod(String className, String friendly, String obf) {
        methods.put(className + "." + friendly, obf);
        return this;
    }

    public SimpleRemap addClass(String friendly, String obf) {
        classes.put(friendly, obf);
        return this;
    }

    // ─────────────────────────────────────────────
    // BUILT-IN: 1.4.7
    // ─────────────────────────────────────────────

    private static SimpleRemap build147() {
        SimpleRemap r = new SimpleRemap("1.4.7");

        // ── Classes ──────────────────────────────────────────
        r.addClass("Minecraft",             "net.minecraft.client.Minecraft");
        r.addClass("EntityPlayer",          "qx");
        r.addClass("EntityPlayerSP",        "ayp");   // client-side player
        r.addClass("EntityPlayerMP",        "iq");    // server-side player (EntityPlayerMP)
        r.addClass("World",                 "yc");
        r.addClass("WorldClient",           "ayp");
        r.addClass("Item",                  "qi");
        r.addClass("ItemStack",             "ql");
        r.addClass("FoodStats",             "ze");
        r.addClass("InventoryPlayer",       "qn");
        r.addClass("NetServerHandler",      "iv");
        r.addClass("INetworkManager",       "ce");   // interface
        r.addClass("TcpConnection",         "cg");
        r.addClass("ServerConfigurationManager", "qo");
        r.addClass("MinecraftServer",       "net.minecraft.server.MinecraftServer");
        r.addClass("IntegratedServer",      "net.minecraft.server.integrated.IntegratedServer");
        r.addClass("Packet250CustomPayload","di");
        r.addClass("Vec3",                  "za");
        r.addClass("FoodAction",            "rp");

        // ── Minecraft fields ─────────────────────────────────
        r.addField("Minecraft", "thePlayer",        "h");
        r.addField("Minecraft", "theWorld",         "f");
        r.addField("Minecraft", "netClientHandler", "q");
        r.addField("Minecraft", "theIntegratedServer", "t");  // may vary

        // ── Minecraft methods ─────────────────────────────────
        r.addMethod("Minecraft", "getInstance",      "x");   // static
        r.addMethod("Minecraft", "getNetClientHandler", "r");

        // ── EntityPlayer / EntityLiving fields ───────────────
        r.addField("EntityPlayer", "inventory",     "bJ");
        r.addField("EntityPlayer", "foodStats",     "bM");   // also try bN, bL
        r.addField("EntityPlayer", "username",      "bR");
        r.addField("EntityPlayer", "worldObj",      "p");
        r.addField("EntityPlayer", "posX",          "t");
        r.addField("EntityPlayer", "posY",          "u");
        r.addField("EntityPlayer", "posZ",          "v");

        // ── EntityPlayer methods ──────────────────────────────
        r.addMethod("EntityPlayer", "getHeldItem",     "bD");   // → ItemStack
        r.addMethod("EntityPlayer", "isUsingItem",     "bM");   // → boolean
        r.addMethod("EntityPlayer", "getUsername",     "an");   // → String  (try also c_)
        r.addMethod("EntityPlayer", "getLookVec",      "Z");    // → Vec3
        r.addMethod("EntityPlayer", "isCreativeMode",  "cf");   // → boolean (true = creative)
        r.addMethod("EntityPlayer", "swingItem",       "bT");
        r.addMethod("EntityPlayer", "getFoodStats",    "cc");   // → FoodStats

        // ── ItemStack fields ──────────────────────────────────
        r.addField("ItemStack", "stackSize",  "a");
        // note: Item reference is retrieved via method, not field

        // ── ItemStack methods ─────────────────────────────────
        r.addMethod("ItemStack", "getItem",       "b");   // → Item
        r.addMethod("ItemStack", "getItemUseAction", "u"); // → EnumAction

        // ── Item fields ───────────────────────────────────────
        r.addField("Item", "itemID",  "cj");

        // ── FoodStats methods ─────────────────────────────────
        r.addMethod("FoodStats", "addStats",  "a");   // (int hunger, float saturation)

        // ── InventoryPlayer fields ────────────────────────────
        r.addField("InventoryPlayer", "mainInventory",    "a");
        r.addField("InventoryPlayer", "currentItem",      "c");
        r.addField("InventoryPlayer", "inventoryChanged", "e");

        // ── EntityPlayerMP (server) fields ───────────────────
        r.addField("EntityPlayerMP", "playerNetServerHandler", "a");  // → NetServerHandler

        // ── NetServerHandler fields ───────────────────────────
        r.addField("NetServerHandler", "netManager",  "b");  // → INetworkManager

        // ── INetworkManager methods ───────────────────────────
        r.addMethod("INetworkManager", "processReadPackets", "b");  // no-arg

        // ── Packet250CustomPayload fields ─────────────────────
        r.addField("Packet250CustomPayload", "channel",  "a");
        r.addField("Packet250CustomPayload", "data",     "c");
        r.addField("Packet250CustomPayload", "length",   "b");

        // ── ServerConfigurationManager methods / fields ───────
        r.addMethod("ServerConfigurationManager", "getPlayerByUsername", "f");   // (String) → EntityPlayerMP
        r.addField("ServerConfigurationManager",  "playerList", "b");            // List<EntityPlayerMP>

        // ── MinecraftServer methods ────────────────────────────
        r.addMethod("MinecraftServer", "getConfigurationManager", "ad");

        // ── Vec3 fields ───────────────────────────────────────
        r.addField("Vec3", "xCoord", "c");
        r.addField("Vec3", "yCoord", "d");
        r.addField("Vec3", "zCoord", "e");

        // ── Sound / playSound method on EntityPlayer ──────────
        r.addMethod("EntityPlayer", "playSound",  "a");  // (String, float, float)

        return r;
    }

    @Override
    public String toString() {
        return "SimpleRemap{version=" + version
                + ", fields=" + fields.size()
                + ", methods=" + methods.size()
                + ", classes=" + classes.size() + "}";
    }
}
