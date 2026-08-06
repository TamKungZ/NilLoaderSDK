package me.tamkungz.nilkit.mapping;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class SrgMappingsTest {

    @Test
    public void readsReversesAndWritesSrg() throws Exception {
        Path in = Files.createTempFile("mapping", ".srg");
        Path out = Files.createTempFile("mapping-reverse", ".srg");
        Files.write(in, (
                "PK: friendly obf\n" +
                "CL: a/A x/Y\n" +
                "FD: a/A/value x/Y/z\n" +
                "MD: a/A/run (La/A;)La/A; x/Y/q (Lx/Y;)Lx/Y;\n"
        ).getBytes(StandardCharsets.UTF_8));

        SrgMappingSet set = SrgMappings.read(in);
        assertEquals("x/Y", set.mapClass("a/A"));
        assertEquals("z", set.mapField("a/A", "value").getTargetName());
        assertEquals("q", set.mapMethod("a/A", "run", "(La/A;)La/A;").getTargetName());
        assertTrue(set.getWarnings().isEmpty());

        SrgMappingSet reverse = SrgMappings.reverse(set);
        assertEquals("a/A", reverse.mapClass("x/Y"));
        assertEquals("run", reverse.mapMethod("x/Y", "q", "(Lx/Y;)Lx/Y;").getTargetName());

        SrgMappings.write(out, reverse);
        SrgMappingSet roundTrip = SrgMappings.read(out);
        assertEquals("a/A", roundTrip.mapClass("x/Y"));
    }

    @Test
    public void chainsTwoNamespaces() throws Exception {
        Path firstFile = Files.createTempFile("mapping-a-b", ".srg");
        Path secondFile = Files.createTempFile("mapping-b-c", ".srg");
        Files.write(firstFile, (
                "CL: a/A b/B\n" +
                "FD: a/A/foo b/B/bar\n" +
                "MD: a/A/run ()La/A; b/B/go ()Lb/B;\n"
        ).getBytes(StandardCharsets.UTF_8));
        Files.write(secondFile, (
                "CL: b/B c/C\n" +
                "FD: b/B/bar c/C/baz\n" +
                "MD: b/B/go ()Lb/B; c/C/doIt ()Lc/C;\n"
        ).getBytes(StandardCharsets.UTF_8));

        SrgMappingSet chained = SrgMappings.chain(SrgMappings.read(firstFile), SrgMappings.read(secondFile));
        assertEquals("c/C", chained.mapClass("a/A"));
        assertEquals("baz", chained.mapField("a/A", "foo").getTargetName());
        assertEquals("doIt", chained.mapMethod("a/A", "run", "()La/A;").getTargetName());
    }

    @Test
    public void readsCompactSrg() throws Exception {
        Path file = Files.createTempFile("mapping", ".csrg");
        Files.write(file, (
                "net/minecraft/server/AABBPool aog\n" +
                "net/minecraft/server/AABBPool largestSize e\n" +
                "net/minecraft/server/AxisAlignedBB aoe\n" +
                "net/minecraft/server/AxisAlignedBB clone ()Lnet/minecraft/server/AxisAlignedBB; c\n"
        ).getBytes(StandardCharsets.UTF_8));

        SrgMappingSet set = SrgMappings.read(file);
        assertEquals("aog", set.mapClass("net/minecraft/server/AABBPool"));
        assertEquals("aog", set.mapField("net/minecraft/server/AABBPool", "largestSize").getTargetOwner());
        assertEquals("()Laoe;", set.mapMethod(
                "net/minecraft/server/AxisAlignedBB", "clone",
                "()Lnet/minecraft/server/AxisAlignedBB;").getTargetDescriptor());
    }

    @Test
    public void compactSrgDoesNotDependOnClassLineOrder() throws Exception {
        Path file = Files.createTempFile("mapping-order", ".csrg");
        Files.write(file, (
                "net/minecraft/server/AxisAlignedBB clone ()Lnet/minecraft/server/AxisAlignedBB; c\n" +
                "net/minecraft/server/AABBPool largestSize e\n" +
                "net/minecraft/server/AABBPool aog\n" +
                "net/minecraft/server/AxisAlignedBB aoe\n"
        ).getBytes(StandardCharsets.UTF_8));

        SrgMappingSet set = SrgMappings.read(file);
        assertEquals("aog", set.mapField("net/minecraft/server/AABBPool", "largestSize").getTargetOwner());
        assertEquals("aoe", set.mapMethod(
                "net/minecraft/server/AxisAlignedBB", "clone",
                "()Lnet/minecraft/server/AxisAlignedBB;").getTargetOwner());
        assertEquals("()Laoe;", set.mapMethod(
                "net/minecraft/server/AxisAlignedBB", "clone",
                "()Lnet/minecraft/server/AxisAlignedBB;").getTargetDescriptor());
    }

    @Test
    public void reportsConflictsInsteadOfSilentlyReplacing() throws Exception {
        Path file = Files.createTempFile("mapping-conflict", ".srg");
        Files.write(file, ("CL: a/A b/B\nCL: a/A c/C\n").getBytes(StandardCharsets.UTF_8));
        SrgMappingSet set = SrgMappings.read(file);
        assertEquals("b/B", set.mapClass("a/A"));
        assertEquals(1, set.getWarnings().size());
    }
}
