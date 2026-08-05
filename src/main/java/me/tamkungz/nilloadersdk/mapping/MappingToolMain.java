package me.tamkungz.nilloadersdk.mapping;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Command-line utilities for local Minecraft SRG mapping workflows. */
public final class MappingToolMain {

    private MappingToolMain() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0 || "help".equals(args[0]) || "--help".equals(args[0]) || "-h".equals(args[0])) {
            usage();
            return;
        }

        String command = args[0];
        if ("inspect".equals(command)) {
            requireArgs(args, 2);
            inspect(Paths.get(args[1]));
        } else if ("reverse".equals(command)) {
            requireArgs(args, 3);
            SrgMappings.write(Paths.get(args[2]), SrgMappings.reverse(SrgMappings.read(Paths.get(args[1]))));
            System.out.println("Wrote reversed mapping: " + args[2]);
        } else if ("chain".equals(command)) {
            requireArgs(args, 4);
            SrgMappingSet chained = SrgMappings.chain(
                    SrgMappings.read(Paths.get(args[1])), SrgMappings.read(Paths.get(args[2])));
            SrgMappings.write(Paths.get(args[3]), chained);
            System.out.println("Wrote chained mapping: " + args[3] + " (" + chained.size() + " entries)");
        } else if ("lookup".equals(command)) {
            lookup(args);
        } else if ("inspect-submodule".equals(command)) {
            inspectSubmodule(args);
        } else if ("submodule-path".equals(command)) {
            printSubmodulePath(args);
        } else if ("list-submodule".equals(command)) {
            listSubmodule(args);
        } else {
            throw new IllegalArgumentException("Unknown command: " + command);
        }
    }

    private static void inspect(Path file) throws IOException {
        SrgMappingSet set = SrgMappings.read(file);
        System.out.println("File      : " + file.toAbsolutePath());
        System.out.println("Packages  : " + set.getPackages().size());
        System.out.println("Classes   : " + set.getClasses().size());
        System.out.println("Fields    : " + set.getFields().size());
        System.out.println("Methods   : " + set.getMethods().size());
        System.out.println("Warnings  : " + set.getWarnings().size());
        for (String warning : set.getWarnings()) System.out.println("  - " + warning);
    }

    private static void lookup(String[] args) throws IOException {
        requireArgs(args, 4);
        SrgMappingSet set = SrgMappings.read(Paths.get(args[1]));
        String type = args[2];
        if ("class".equals(type)) {
            String mapped = set.mapClass(args[3]);
            System.out.println(mapped == null ? "<not found>" : mapped);
            return;
        }
        if ("field".equals(type)) {
            requireArgs(args, 5);
            SrgMappingSet.MemberMapping m = set.mapField(args[3], args[4]);
            System.out.println(m == null ? "<not found>" : m.getTargetOwner() + "/" + m.getTargetName());
            return;
        }
        if ("method".equals(type)) {
            requireArgs(args, 6);
            SrgMappingSet.MethodMapping m = set.mapMethod(args[3], args[4], args[5]);
            System.out.println(m == null ? "<not found>"
                    : m.getTargetOwner() + "/" + m.getTargetName() + " " + m.getTargetDescriptor());
            return;
        }
        throw new IllegalArgumentException("lookup type must be class, field, or method");
    }

    private static Path submoduleFile(String[] args) throws IOException {
        requireArgs(args, 2);
        String version = args[1];
        String fileName = args.length >= 3 ? args[2] : "mcp2obf.srg";
        validateSimpleSegment(version, "version");
        validateSimpleSegment(fileName, "mapping file");

        Path source = Paths.get("tools", "MinecraftRemapping", version, fileName);
        if (!Files.isRegularFile(source)) {
            throw new IOException("Mapping file not found in submodule: " + source
                    + "\nRun git submodule update --init --recursive first.");
        }
        return source;
    }

    private static void inspectSubmodule(String[] args) throws IOException {
        inspect(submoduleFile(args));
    }

    private static void printSubmodulePath(String[] args) throws IOException {
        System.out.println(submoduleFile(args).toAbsolutePath());
    }

    private static void listSubmodule(String[] args) throws IOException {
        Path root = Paths.get("tools", "MinecraftRemapping");
        if (!Files.isDirectory(root)) {
            throw new IOException("MinecraftRemapping submodule is not initialized: " + root);
        }
        if (args.length >= 2) {
            validateSimpleSegment(args[1], "version");
            listFiles(root.resolve(args[1]));
            return;
        }

        List<String> versions = new ArrayList<String>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
            for (Path p : stream) if (Files.isDirectory(p)) versions.add(p.getFileName().toString());
        }
        Collections.sort(versions);
        for (String version : versions) System.out.println(version);
    }

    private static void listFiles(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) throw new IOException("Version directory not found: " + dir);
        List<String> files = new ArrayList<String>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path p : stream) if (Files.isRegularFile(p)) files.add(p.getFileName().toString());
        }
        Collections.sort(files);
        for (String file : files) System.out.println(file);
    }

    private static void validateSimpleSegment(String value, String label) {
        if (value == null || value.isEmpty() || value.contains("/") || value.contains("\\") || value.contains("..")) {
            throw new IllegalArgumentException("Invalid " + label + ": " + value);
        }
    }

    private static void requireArgs(String[] args, int count) {
        if (args.length < count) {
            usage();
            throw new IllegalArgumentException("Not enough arguments for " + args[0]);
        }
    }

    private static void usage() {
        System.out.println("NilLoaderSDK mapping tool");
        System.out.println("  inspect <file>");
        System.out.println("  reverse <input> <output>");
        System.out.println("  chain <first> <second> <output>");
        System.out.println("  lookup <file> class <internalName>");
        System.out.println("  lookup <file> field <owner> <name>");
        System.out.println("  lookup <file> method <owner> <name> <descriptor>");
        System.out.println("  list-submodule [version]");
        System.out.println("  inspect-submodule <version> [mappingFile=mcp2obf.srg]");
        System.out.println("  submodule-path <version> [mappingFile=mcp2obf.srg]");
    }
}
