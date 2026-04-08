package me.tamkungz.remapping;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
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
 * Usage:
 *   SimpleRemap remap = SimpleRemap.forVersion("1.4.7");
 *   String fieldName = remap.field("EntityPlayer", "inventory");
 *   String methodName = remap.method("EntityPlayer", "getHeldItem");
 *
 * Sources:
 *   1) Bundled/local SRG files
 *   2) Built-in manual mappings
 *
 * Priority in forVersion():
 *   external SRG -> built-in manual overrides
 */
public final class SimpleRemap {

    private static final String PRIMARY_SRG = "mcp2obf.srg";
    private static final String FALLBACK_SRG = "obf2mcp.srg";

    private final String version;
    private final Map<String, String> fields = new HashMap<>();
    private final Map<String, String> methods = new HashMap<>();
    private final Map<String, String> classes = new HashMap<>();

    private SimpleRemap(String version) {
        this.version = version;
    }

    // ─────────────────────────────────────────────
    // FACTORY
    // ─────────────────────────────────────────────

    /**
     * Creates a remap for a version by combining:
     *   1) generated / SRG mappings
     *   2) built-in manual overrides
     *
     * Built-in mappings are applied last so manual fixes win.
     */
    public static SimpleRemap forVersion(String version) {
		SimpleRemap r = empty(version);

		boolean hasGenerated = applyGenerated(version, r, true);
		boolean hasBuiltin = applyBuiltin(version, r, true);

		if (!hasGenerated && !hasBuiltin) {
			throw new IllegalArgumentException(
					"No mappings found for version: " + version +
					" (expected built-in mappings or local SRG files at .remapping/" + version + "/)"
			);
		}

		return r;
	}

    /** Creates a remap using built-in manual mappings only. */
	public static SimpleRemap builtinOnly(String version) {
		SimpleRemap r = empty(version);
		if (!applyBuiltin(version, r, true)) {
			throw new IllegalArgumentException("No built-in mappings for version: " + version);
		}
		return r;
	}

    /** Creates a remap using local/bundled SRG mappings only. */
	public static SimpleRemap generatedOnly(String version) {
		SimpleRemap r = empty(version);
		if (!applyGenerated(version, r, true)) {
			throw new IllegalArgumentException(
					"No external SRG mappings found for version: " + version +
					" (expected " + PRIMARY_SRG + " or " + FALLBACK_SRG + ")"
			);
		}
		return r;
	}

    /** Creates an empty remap for custom or unsupported versions. */
	public static SimpleRemap empty(String version) {
		return new SimpleRemap(version);
	}

    public static boolean applyBuiltin(String version, SimpleRemap remap, boolean overwriteExisting) {
		switch (version) {
			case "1.4.7":
				applyBuiltin147(remap, overwriteExisting);
				return true;
			default:
				return false;
		}
	}

    public static boolean applyGenerated(String version, SimpleRemap remap, boolean overwriteExisting) {
		// 1) Bundled resources inside JAR
		if (loadBundledSrgIfPresent(remap, version, overwriteExisting)) {
			return true;
		}

		// 2) Local development files
		return loadLocalSrgIfPresent(remap, version, overwriteExisting);
	}

    public boolean isEmpty() {
        return fields.isEmpty() && methods.isEmpty() && classes.isEmpty();
    }

    // ─────────────────────────────────────────────
    // LOOKUP
    // ─────────────────────────────────────────────

    /**
     * Returns obfuscated field name.
     * Key format: "ClassName.fieldName"
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

    public String getVersion() {
        return version;
    }

    // ─────────────────────────────────────────────
    // SRG LOADING
    // ─────────────────────────────────────────────

    private static boolean loadBundledSrgIfPresent(SimpleRemap r, String version, boolean overwriteExisting) {
        String primaryResource = "/remapping/" + version + "/" + PRIMARY_SRG;
        if (parseResourceIfPresent(primaryResource, r, overwriteExisting, false)) {
            return true;
        }

        String fallbackResource = "/remapping/" + version + "/" + FALLBACK_SRG;
        return parseResourceIfPresent(fallbackResource, r, overwriteExisting, true);
    }

    private static boolean parseResourceIfPresent(String resourcePath, SimpleRemap r, boolean overwriteExisting, boolean reverse) {
        try (InputStream in = SimpleRemap.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                return false;
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                parseSrg(br, r, overwriteExisting, reverse);
                return true;
            }
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean loadLocalSrgIfPresent(SimpleRemap r, String version, boolean overwriteExisting) {
        File primary = new File(".remapping/" + version + "/" + PRIMARY_SRG);
        if (primary.exists() && primary.isFile()) {
            try (BufferedReader br = new BufferedReader(new FileReader(primary))) {
                parseSrg(br, r, overwriteExisting, false);
                return true;
            } catch (Exception ignored) {
                return false;
            }
        }

        File fallback = new File(".remapping/" + version + "/" + FALLBACK_SRG);
        if (fallback.exists() && fallback.isFile()) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    new FileInputStream(fallback), StandardCharsets.UTF_8))) {
                parseSrg(br, r, overwriteExisting, true);
                return true;
            } catch (Exception ignored) {
                return false;
            }
        }

        return false;
    }

    private static void parseSrg(BufferedReader br, SimpleRemap r, boolean overwriteExisting, boolean reverse) throws Exception {
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            if (line.startsWith("CL: ")) {
                String[] p = line.substring(4).trim().split("\\s+");
                if (p.length >= 2) {
                    String left = p[0];
                    String right = p[1];

                    String friendlyPath = reverse ? right : left;
                    String obfPath = reverse ? left : right;

                    String friendly = simpleClass(friendlyPath);
                    String obf = normalizeClass(obfPath);

                    r.addClass(friendly, obf, overwriteExisting);
                }
            } else if (line.startsWith("FD: ")) {
                String[] p = line.substring(4).trim().split("\\s+");
                if (p.length >= 2) {
                    String[] left = splitOwnerName(p[0]);
                    String[] right = splitOwnerName(p[1]);
                    if (left != null && right != null) {
                        String[] friendlySide = reverse ? right : left;
                        String[] obfSide = reverse ? left : right;

                        r.addField(
                                simpleClass(friendlySide[0]),
                                friendlySide[1],
                                obfSide[1],
                                overwriteExisting
                        );
                    }
                }
            } else if (line.startsWith("MD: ")) {
                String[] p = line.substring(4).trim().split("\\s+");
                if (p.length >= 4) {
                    String[] left = splitOwnerName(p[0]);
                    String[] right = splitOwnerName(p[2]);
                    if (left != null && right != null) {
                        String[] friendlySide = reverse ? right : left;
                        String[] obfSide = reverse ? left : right;

                        r.addMethod(
                                simpleClass(friendlySide[0]),
                                friendlySide[1],
                                obfSide[1],
                                overwriteExisting
                        );
                    }
                }
            }
        }
    }

    private static String normalizeClass(String internalName) {
        if (internalName == null) {
            return null;
        }
        return internalName.indexOf('/') >= 0 ? internalName.replace('/', '.') : internalName;
    }

    private static String simpleClass(String internalName) {
        if (internalName == null) {
            return null;
        }
        int idx = internalName.lastIndexOf('/');
        return idx >= 0 ? internalName.substring(idx + 1) : internalName;
    }

    private static String[] splitOwnerName(String ownerAndName) {
        int idx = ownerAndName.lastIndexOf('/');
        if (idx <= 0 || idx >= ownerAndName.length() - 1) {
            return null;
        }
        return new String[] {
                ownerAndName.substring(0, idx),
                ownerAndName.substring(idx + 1)
        };
    }

    // ─────────────────────────────────────────────
    // BUILDER HELPERS
    // ─────────────────────────────────────────────

    public SimpleRemap addField(String className, String friendly, String obf) {
        return addField(className, friendly, obf, true);
    }

    public SimpleRemap addField(String className, String friendly, String obf, boolean overwriteExisting) {
        String key = className + "." + friendly;
        if (overwriteExisting) {
            fields.put(key, obf);
        } else {
            fields.putIfAbsent(key, obf);
        }
        return this;
    }

    public SimpleRemap addMethod(String className, String friendly, String obf) {
        return addMethod(className, friendly, obf, true);
    }

    public SimpleRemap addMethod(String className, String friendly, String obf, boolean overwriteExisting) {
        String key = className + "." + friendly;
        if (overwriteExisting) {
            methods.put(key, obf);
        } else {
            methods.putIfAbsent(key, obf);
        }
        return this;
    }

    public SimpleRemap addClass(String friendly, String obf) {
        return addClass(friendly, obf, true);
    }

    public SimpleRemap addClass(String friendly, String obf, boolean overwriteExisting) {
        if (overwriteExisting) {
            classes.put(friendly, obf);
        } else {
            classes.putIfAbsent(friendly, obf);
        }
        return this;
    }

    // ─────────────────────────────────────────────
    // BUILT-IN: 1.4.7
    // ─────────────────────────────────────────────

    private static void applyBuiltin147(SimpleRemap r, boolean overwriteExisting) {
        // ── Classes ──────────────────────────────────────────
        r.addClass("Minecraft", "net.minecraft.client.Minecraft", overwriteExisting);
        r.addClass("EntityPlayer", "qx", overwriteExisting);
        r.addClass("EntityPlayerSP", "ayp", overwriteExisting);
        r.addClass("EntityPlayerMP", "iq", overwriteExisting);
        r.addClass("World", "yc", overwriteExisting);
        r.addClass("WorldClient", "ayp", overwriteExisting);
        r.addClass("Item", "qi", overwriteExisting);
        r.addClass("ItemStack", "ql", overwriteExisting);
        r.addClass("FoodStats", "ze", overwriteExisting);
        r.addClass("InventoryPlayer", "qn", overwriteExisting);
        r.addClass("NetServerHandler", "iv", overwriteExisting);
        r.addClass("INetworkManager", "ce", overwriteExisting);
        r.addClass("TcpConnection", "cg", overwriteExisting);
        r.addClass("ServerConfigurationManager", "qo", overwriteExisting);
        r.addClass("MinecraftServer", "net.minecraft.server.MinecraftServer", overwriteExisting);
        r.addClass("IntegratedServer", "net.minecraft.server.integrated.IntegratedServer", overwriteExisting);
        r.addClass("Packet250CustomPayload", "di", overwriteExisting);
        r.addClass("Vec3", "za", overwriteExisting);
        r.addClass("FoodAction", "rp", overwriteExisting);

        // ── Minecraft fields ─────────────────────────────────
        r.addField("Minecraft", "thePlayer", "h", overwriteExisting);
        r.addField("Minecraft", "theWorld", "f", overwriteExisting);
        r.addField("Minecraft", "netClientHandler", "q", overwriteExisting);
        r.addField("Minecraft", "theIntegratedServer", "t", overwriteExisting);

        // ── Minecraft methods ────────────────────────────────
        r.addMethod("Minecraft", "getInstance", "x", overwriteExisting);
        r.addMethod("Minecraft", "getNetClientHandler", "r", overwriteExisting);

        // ── EntityPlayer / EntityLiving fields ──────────────
        r.addField("EntityPlayer", "inventory", "bJ", overwriteExisting);
        r.addField("EntityPlayer", "foodStats", "bM", overwriteExisting);
        r.addField("EntityPlayer", "username", "bR", overwriteExisting);
        r.addField("EntityPlayer", "worldObj", "p", overwriteExisting);
        r.addField("EntityPlayer", "posX", "t", overwriteExisting);
        r.addField("EntityPlayer", "posY", "u", overwriteExisting);
        r.addField("EntityPlayer", "posZ", "v", overwriteExisting);

        // ── EntityPlayer methods ─────────────────────────────
        r.addMethod("EntityPlayer", "getHeldItem", "bD", overwriteExisting);
        r.addMethod("EntityPlayer", "isUsingItem", "bM", overwriteExisting);
        r.addMethod("EntityPlayer", "getUsername", "an", overwriteExisting);
        r.addMethod("EntityPlayer", "getLookVec", "Z", overwriteExisting);
        r.addMethod("EntityPlayer", "isCreativeMode", "cf", overwriteExisting);
        r.addMethod("EntityPlayer", "swingItem", "bT", overwriteExisting);
        r.addMethod("EntityPlayer", "getFoodStats", "cc", overwriteExisting);

        // ── ItemStack fields / methods ───────────────────────
        r.addField("ItemStack", "stackSize", "a", overwriteExisting);
        r.addMethod("ItemStack", "getItem", "b", overwriteExisting);
        r.addMethod("ItemStack", "getItemUseAction", "u", overwriteExisting);

        // ── Item fields ──────────────────────────────────────
        r.addField("Item", "itemID", "cj", overwriteExisting);

        // ── FoodStats methods ────────────────────────────────
        r.addMethod("FoodStats", "addStats", "a", overwriteExisting);

        // ── InventoryPlayer fields ───────────────────────────
        r.addField("InventoryPlayer", "mainInventory", "a", overwriteExisting);
        r.addField("InventoryPlayer", "currentItem", "c", overwriteExisting);
        r.addField("InventoryPlayer", "inventoryChanged", "e", overwriteExisting);

        // ── EntityPlayerMP / networking ──────────────────────
        r.addField("EntityPlayerMP", "playerNetServerHandler", "a", overwriteExisting);
        r.addField("NetServerHandler", "netManager", "b", overwriteExisting);
        r.addMethod("INetworkManager", "processReadPackets", "b", overwriteExisting);

        // ── Packet250CustomPayload fields ────────────────────
        r.addField("Packet250CustomPayload", "channel", "a", overwriteExisting);
        r.addField("Packet250CustomPayload", "data", "c", overwriteExisting);
        r.addField("Packet250CustomPayload", "length", "b", overwriteExisting);

        // ── ServerConfigurationManager / server ──────────────
        r.addMethod("ServerConfigurationManager", "getPlayerByUsername", "f", overwriteExisting);
        r.addField("ServerConfigurationManager", "playerList", "b", overwriteExisting);
        r.addMethod("MinecraftServer", "getConfigurationManager", "ad", overwriteExisting);

        // ── Vec3 fields ──────────────────────────────────────
        r.addField("Vec3", "xCoord", "c", overwriteExisting);
        r.addField("Vec3", "yCoord", "d", overwriteExisting);
        r.addField("Vec3", "zCoord", "e", overwriteExisting);

        // ── Misc methods ─────────────────────────────────────
        r.addMethod("EntityPlayer", "playSound", "a", overwriteExisting);
    }

    @Override
    public String toString() {
        return "SimpleRemap{version=" + version
                + ", fields=" + fields.size()
                + ", methods=" + methods.size()
                + ", classes=" + classes.size() + "}";
    }
}