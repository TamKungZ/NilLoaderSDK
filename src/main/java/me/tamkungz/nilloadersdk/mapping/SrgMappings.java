package me.tamkungz.nilloadersdk.mapping;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Utilities for reading, writing, reversing and composing SRG mapping files. */
public final class SrgMappings {

    private SrgMappings() {
    }

    public static SrgMappingSet read(Path file) throws IOException {
        SrgMappingSet set = new SrgMappingSet();
        List<SourceLine> lines = new ArrayList<SourceLine>();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String raw;
            int lineNo = 0;
            while ((raw = reader.readLine()) != null) {
                lineNo++;
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                lines.add(new SourceLine(line, lineNo));
            }
        }

        // Compact SRG (CSRG) members only store the target member name. Resolve all
        // class mappings first so target owners and method descriptors do not depend
        // on the order of entries in the source file.
        for (SourceLine sourceLine : lines) {
            if (isNamespaceDeclaration(sourceLine.text)) {
                parseLine(set, sourceLine.text, sourceLine.number);
            }
        }
        for (SourceLine sourceLine : lines) {
            if (!isNamespaceDeclaration(sourceLine.text)) {
                parseLine(set, sourceLine.text, sourceLine.number);
            }
        }
        return set;
    }

    private static boolean isNamespaceDeclaration(String line) {
        if (line.startsWith("PK: ") || line.startsWith("CL: ")) return true;
        if (line.startsWith("FD: ") || line.startsWith("MD: ")) return false;
        return tokens(line).length == 2;
    }

    private static void parseLine(SrgMappingSet set, String line, int lineNo) {
        if (line.startsWith("PK: ")) {
            String[] p = tokens(line.substring(4));
            if (p.length >= 2) set.putPackage(p[0], p[1], lineNo);
            else set.warn("line " + lineNo + ": malformed PK entry");
            return;
        }
        if (line.startsWith("CL: ")) {
            String[] p = tokens(line.substring(4));
            if (p.length >= 2) set.putClass(p[0], p[1], lineNo);
            else set.warn("line " + lineNo + ": malformed CL entry");
            return;
        }
        if (line.startsWith("FD: ")) {
            String[] p = tokens(line.substring(4));
            if (p.length >= 2) {
                String[] left = splitOwnerName(p[0]);
                String[] right = splitOwnerName(p[1]);
                if (left != null && right != null) {
                    set.putField(new SrgMappingSet.MemberMapping(left[0], left[1], right[0], right[1]), lineNo);
                } else {
                    set.warn("line " + lineNo + ": malformed FD owner/name");
                }
            } else set.warn("line " + lineNo + ": malformed FD entry");
            return;
        }
        if (line.startsWith("MD: ")) {
            String[] p = tokens(line.substring(4));
            if (p.length >= 4) {
                String[] left = splitOwnerName(p[0]);
                String[] right = splitOwnerName(p[2]);
                if (left != null && right != null) {
                    set.putMethod(new SrgMappingSet.MethodMapping(
                            left[0], left[1], p[1], right[0], right[1], p[3]), lineNo);
                } else {
                    set.warn("line " + lineNo + ": malformed MD owner/name");
                }
            } else set.warn("line " + lineNo + ": malformed MD entry");
            return;
        }

        // CSRG compatibility: class lines have 2 tokens; field lines have 3; method lines have 4.
        String[] p = tokens(line);
        if (p.length == 2) {
            set.putClass(p[0], p[1], lineNo);
        } else if (p.length == 3) {
            String targetOwner = set.mapClass(p[0]);
            if (targetOwner == null) targetOwner = p[0];
            set.putField(new SrgMappingSet.MemberMapping(p[0], p[1], targetOwner, p[2]), lineNo);
        } else if (p.length == 4 && p[2].startsWith("(")) {
            String targetOwner = set.mapClass(p[0]);
            if (targetOwner == null) targetOwner = p[0];
            String targetDescriptor = remapDescriptor(p[2], set.getClasses());
            set.putMethod(new SrgMappingSet.MethodMapping(p[0], p[1], p[2], targetOwner, p[3], targetDescriptor), lineNo);
        } else {
            set.warn("line " + lineNo + ": unsupported mapping syntax: " + line);
        }
    }

    public static void write(Path file, SrgMappingSet set) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            List<Map.Entry<String, String>> packages = new ArrayList<Map.Entry<String, String>>(set.getPackages().entrySet());
            sortEntries(packages);
            for (Map.Entry<String, String> e : packages) {
                writer.write("PK: " + e.getKey() + " " + e.getValue());
                writer.newLine();
            }

            List<Map.Entry<String, String>> classes = new ArrayList<Map.Entry<String, String>>(set.getClasses().entrySet());
            sortEntries(classes);
            for (Map.Entry<String, String> e : classes) {
                writer.write("CL: " + e.getKey() + " " + e.getValue());
                writer.newLine();
            }

            List<SrgMappingSet.MemberMapping> fields = new ArrayList<SrgMappingSet.MemberMapping>(set.getFields());
            Collections.sort(fields, new Comparator<SrgMappingSet.MemberMapping>() {
                @Override
                public int compare(SrgMappingSet.MemberMapping a, SrgMappingSet.MemberMapping b) {
                    return a.sourceKey().compareTo(b.sourceKey());
                }
            });
            for (SrgMappingSet.MemberMapping f : fields) {
                writer.write("FD: " + f.getSourceOwner() + "/" + f.getSourceName()
                        + " " + f.getTargetOwner() + "/" + f.getTargetName());
                writer.newLine();
            }

            List<SrgMappingSet.MethodMapping> methods = new ArrayList<SrgMappingSet.MethodMapping>(set.getMethods());
            Collections.sort(methods, new Comparator<SrgMappingSet.MethodMapping>() {
                @Override
                public int compare(SrgMappingSet.MethodMapping a, SrgMappingSet.MethodMapping b) {
                    return a.sourceKey().compareTo(b.sourceKey());
                }
            });
            for (SrgMappingSet.MethodMapping m : methods) {
                writer.write("MD: " + m.getSourceOwner() + "/" + m.getSourceName() + " " + m.getSourceDescriptor()
                        + " " + m.getTargetOwner() + "/" + m.getTargetName() + " " + m.getTargetDescriptor());
                writer.newLine();
            }
        }
    }

    public static SrgMappingSet reverse(SrgMappingSet input) {
        SrgMappingSet out = new SrgMappingSet();
        for (Map.Entry<String, String> e : input.getPackages().entrySet()) out.putPackage(e.getValue(), e.getKey(), 0);
        for (Map.Entry<String, String> e : input.getClasses().entrySet()) out.putClass(e.getValue(), e.getKey(), 0);
        for (SrgMappingSet.MemberMapping f : input.getFields()) {
            out.putField(new SrgMappingSet.MemberMapping(
                    f.getTargetOwner(), f.getTargetName(), f.getSourceOwner(), f.getSourceName()), 0);
        }
        for (SrgMappingSet.MethodMapping m : input.getMethods()) {
            out.putMethod(new SrgMappingSet.MethodMapping(
                    m.getTargetOwner(), m.getTargetName(), m.getTargetDescriptor(),
                    m.getSourceOwner(), m.getSourceName(), m.getSourceDescriptor()), 0);
        }
        return out;
    }

    /**
     * Composes A->B and B->C mappings into A->C. Entries without an exact second-stage match are omitted.
     */
    public static SrgMappingSet chain(SrgMappingSet first, SrgMappingSet second) {
        SrgMappingSet out = new SrgMappingSet();

        for (Map.Entry<String, String> e : first.getPackages().entrySet()) {
            String target = second.getPackages().get(e.getValue());
            if (target != null) out.putPackage(e.getKey(), target, 0);
        }
        for (Map.Entry<String, String> e : first.getClasses().entrySet()) {
            String target = second.getClasses().get(e.getValue());
            if (target != null) out.putClass(e.getKey(), target, 0);
        }

        Map<String, SrgMappingSet.MemberMapping> secondFields = new HashMap<String, SrgMappingSet.MemberMapping>();
        for (SrgMappingSet.MemberMapping f : second.getFields()) secondFields.put(f.sourceKey(), f);
        for (SrgMappingSet.MemberMapping f : first.getFields()) {
            SrgMappingSet.MemberMapping next = secondFields.get(f.targetKey());
            if (next != null) {
                out.putField(new SrgMappingSet.MemberMapping(
                        f.getSourceOwner(), f.getSourceName(), next.getTargetOwner(), next.getTargetName()), 0);
            }
        }

        Map<String, SrgMappingSet.MethodMapping> secondMethods = new HashMap<String, SrgMappingSet.MethodMapping>();
        for (SrgMappingSet.MethodMapping m : second.getMethods()) secondMethods.put(m.sourceKey(), m);
        for (SrgMappingSet.MethodMapping m : first.getMethods()) {
            SrgMappingSet.MethodMapping next = secondMethods.get(m.targetKey());
            if (next != null) {
                out.putMethod(new SrgMappingSet.MethodMapping(
                        m.getSourceOwner(), m.getSourceName(), m.getSourceDescriptor(),
                        next.getTargetOwner(), next.getTargetName(), next.getTargetDescriptor()), 0);
            }
        }
        return out;
    }

    public static String remapDescriptor(String descriptor, Map<String, String> classes) {
        if (descriptor == null || descriptor.indexOf('L') < 0 || classes.isEmpty()) return descriptor;
        StringBuilder out = new StringBuilder(descriptor.length());
        int i = 0;
        while (i < descriptor.length()) {
            char c = descriptor.charAt(i);
            if (c != 'L') {
                out.append(c);
                i++;
                continue;
            }
            int end = descriptor.indexOf(';', i);
            if (end < 0) return descriptor;
            String name = descriptor.substring(i + 1, end);
            String mapped = classes.get(name);
            out.append('L').append(mapped == null ? name : mapped).append(';');
            i = end + 1;
        }
        return out.toString();
    }

    private static final class SourceLine {
        final String text;
        final int number;

        SourceLine(String text, int number) {
            this.text = text;
            this.number = number;
        }
    }

    private static String[] tokens(String body) {
        String trimmed = body.trim();
        return trimmed.isEmpty() ? new String[0] : trimmed.split("\\s+");
    }

    private static String[] splitOwnerName(String path) {
        int slash = path.lastIndexOf('/');
        if (slash <= 0 || slash == path.length() - 1) return null;
        return new String[] { path.substring(0, slash), path.substring(slash + 1) };
    }

    private static void sortEntries(List<Map.Entry<String, String>> entries) {
        Collections.sort(entries, new Comparator<Map.Entry<String, String>>() {
            @Override
            public int compare(Map.Entry<String, String> a, Map.Entry<String, String> b) {
                return a.getKey().compareTo(b.getKey());
            }
        });
    }
}
