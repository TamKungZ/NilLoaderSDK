package me.tamkungz.nilloadersdk.metadata;

import me.tamkungz.nilloadersdk.helper.TransformerHelper;
import nilloader.api.lib.asm.Opcodes;
import nilloader.api.lib.asm.tree.InsnList;
import nilloader.api.lib.asm.tree.MethodInsnNode;
import nilloader.api.lib.asm.tree.MethodNode;
import nilloader.api.lib.asm.tree.VarInsnNode;

public final class NilMetadataPatchInstaller {

    private static volatile boolean installed;

    private NilMetadataPatchInstaller() {}

    public static synchronized void install() {
        if (installed) return;
        installed = true;

        TransformerHelper.registerAsmPatch("nilloader.api.NilMetadata", (loader, classNode) -> {
            for (MethodNode mn : classNode.methods) {
                if (!"from".equals(mn.name)) continue;
                if (!"(Ljava/lang/String;Lnilloader/api/lib/qdcss/QDCSS;Ljava/io/File;)Lnilloader/api/NilMetadata;".equals(mn.desc)) continue;

                InsnList il = new InsnList();
                il.add(new VarInsnNode(Opcodes.ALOAD, 0));
                il.add(new VarInsnNode(Opcodes.ALOAD, 1));
                il.add(new VarInsnNode(Opcodes.ALOAD, 2));
                il.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "me/tamkungz/nilloadersdk/metadata/NilMetadataBridge",
                        "from",
                        "(Ljava/lang/String;Lnilloader/api/lib/qdcss/QDCSS;Ljava/io/File;)Lnilloader/api/NilMetadata;",
                        false
                ));
                il.add(new nilloader.api.lib.asm.tree.InsnNode(Opcodes.ARETURN));

                mn.instructions.clear();
                mn.instructions.add(il);
                mn.tryCatchBlocks.clear();
                mn.localVariables = null;
                mn.maxStack = 3;
                mn.maxLocals = 3;
                return true;
            }
            return false;
        });
    }
}

