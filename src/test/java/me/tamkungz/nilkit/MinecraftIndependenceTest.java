package me.tamkungz.nilkit;

import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/** Regression guard: SDK classes must not hard-link Minecraft classes in bytecode. */
public class MinecraftIndependenceTest {

    @Test
    public void minecraftFacingSdkClassesHaveNoHardMinecraftTypeReferences() throws Exception {
        String[] resources = {
                "/me/tamkungz/nilkit/helper/McHelper.class",
                "/me/tamkungz/nilkit/helper/PacketHelper.class",
                "/me/tamkungz/nilkit/network/MinecraftAutoNetworkBridge.class",
                "/me/tamkungz/nilkit/util/TargetFinder.class",
                "/me/tamkungz/nilkit/entrypoint/DefaultSdkEntrypointModule.class"
        };

        for (String resource : resources) {
            assertFalse(resource + " contains a hard net.minecraft type reference",
                    hasMinecraftClassConstant(resource));
        }
    }

    private static boolean hasMinecraftClassConstant(String resource) throws IOException {
        InputStream raw = MinecraftIndependenceTest.class.getResourceAsStream(resource);
        assertNotNull("Missing compiled class resource " + resource, raw);

        try (DataInputStream in = new DataInputStream(new BufferedInputStream(raw))) {
            if (in.readInt() != 0xCAFEBABE) {
                throw new IOException("Not a class file: " + resource);
            }
            in.readUnsignedShort(); // minor
            in.readUnsignedShort(); // major

            int count = in.readUnsignedShort();
            String[] utf8 = new String[count];
            int[] classNameIndex = new int[count];

            for (int i = 1; i < count; i++) {
                int tag = in.readUnsignedByte();
                switch (tag) {
                    case 1: // Utf8
                        utf8[i] = in.readUTF();
                        break;
                    case 3: // Integer
                    case 4: // Float
                        in.skipBytes(4);
                        break;
                    case 5: // Long
                    case 6: // Double
                        in.skipBytes(8);
                        i++; // takes two constant-pool slots
                        break;
                    case 7: // Class
                        classNameIndex[i] = in.readUnsignedShort();
                        break;
                    case 8: // String
                    case 16: // MethodType
                    case 19: // Module
                    case 20: // Package
                        in.skipBytes(2);
                        break;
                    case 9:  // Fieldref
                    case 10: // Methodref
                    case 11: // InterfaceMethodref
                    case 12: // NameAndType
                    case 17: // Dynamic
                    case 18: // InvokeDynamic
                        in.skipBytes(4);
                        break;
                    case 15: // MethodHandle
                        in.skipBytes(3);
                        break;
                    default:
                        throw new IOException("Unsupported constant-pool tag " + tag + " in " + resource);
                }
            }

            for (int index : classNameIndex) {
                if (index <= 0 || index >= utf8.length) continue;
                String name = utf8[index];
                if (name != null && name.contains("net/minecraft/")) {
                    return true;
                }
            }
            return false;
        }
    }
}
