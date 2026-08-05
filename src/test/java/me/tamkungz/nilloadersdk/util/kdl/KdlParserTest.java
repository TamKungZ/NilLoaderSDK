package me.tamkungz.nilloadersdk.util.kdl;

import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assert.*;

public class KdlParserTest {

    @Test
    public void parsesCommonKdl2ScalarsAndNumbers() {
        KdlNode node = new KdlParser("test #true #false #null 0xff 0o17 0b1010 1_000 #inf #-inf #nan\n")
                .parse().getNodes().get(0);

        assertEquals(Boolean.TRUE, node.getArguments().get(0).getValue());
        assertEquals(Boolean.FALSE, node.getArguments().get(1).getValue());
        assertNull(node.getArguments().get(2).getValue());
        assertEquals(new BigInteger("255"), node.getArguments().get(3).getValue());
        assertEquals(new BigInteger("15"), node.getArguments().get(4).getValue());
        assertEquals(new BigInteger("10"), node.getArguments().get(5).getValue());
        assertEquals(new BigInteger("1000"), node.getArguments().get(6).getValue());
        assertEquals(Double.POSITIVE_INFINITY, node.getArguments().get(7).getValue());
        assertEquals(Double.NEGATIVE_INFINITY, node.getArguments().get(8).getValue());
        assertTrue(Double.isNaN(((Number) node.getArguments().get(9).getValue()).doubleValue()));
    }

    @Test
    public void writerOutputCanBeParsedAgain() {
        KdlDocument document = new KdlDocument();
        KdlNode node = new KdlNode("entrypoints.premain");
        node.setProperty("quoted key", new KdlValue.KdlBoolean(false));
        node.addArgument(new KdlValue.KdlString("hello\nworld"));
        document.addNode(node);

        String serialized = new KdlWriter().write(document);
        KdlNode reparsed = new KdlParser(serialized).parse().getNodes().get(0);

        assertEquals("entrypoints.premain", reparsed.getName());
        assertEquals("hello\nworld", reparsed.getArguments().get(0).getValue());
        assertEquals(Boolean.FALSE, reparsed.getProperties().get("quoted key").getValue());
    }

    @Test
    public void slashDashDiscardsNodesAndEntries() {
        KdlDocument document = new KdlParser("/- hidden 1\nshown /- 10 20 kept=#true /- removed=#false\n").parse();
        assertEquals(1, document.getNodes().size());
        KdlNode shown = document.getNodes().get(0);
        assertEquals("shown", shown.getName());
        assertEquals(1, shown.getArguments().size());
        assertEquals(new BigInteger("20"), shown.getArguments().get(0).getValue());
        assertEquals(Boolean.TRUE, shown.getProperties().get("kept").getValue());
        assertFalse(shown.getProperties().containsKey("removed"));
    }

    @Test
    public void parsesKdl2RawString() {
        KdlNode node = new KdlParser("raw #\"a \\\"quoted\\\" value\"#\n").parse().getNodes().get(0);
        assertEquals("a \\\"quoted\\\" value", node.getArguments().get(0).getValue());
    }
}
