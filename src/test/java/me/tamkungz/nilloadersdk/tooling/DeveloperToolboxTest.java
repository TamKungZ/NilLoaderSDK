package me.tamkungz.nilloadersdk.tooling;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class DeveloperToolboxTest {

    public static class SampleBase {
        public SampleBase() {
        }
    }

    @Test
    public void bundledToolingIsVisibleToTests() {
        assertTrue(DeveloperToolbox.hasByteBuddy());
        assertTrue(DeveloperToolbox.hasByteBuddyAgent());
        assertTrue(DeveloperToolbox.hasGeb());
        assertTrue(DeveloperToolbox.hasClassGraph());
        assertTrue(DeveloperToolbox.hasSnakeYaml());
    }

    @Test
    public void yamlRoundTripsSimpleMap() {
        Map<String, Object> input = new LinkedHashMap<String, Object>();
        input.put("name", "NilLoaderSDK");
        input.put("enabled", Boolean.TRUE);

        String yaml = YamlHelper.dump(input);
        Map<String, Object> output = YamlHelper.loadMap(yaml);

        assertEquals("NilLoaderSDK", output.get("name"));
        assertEquals(Boolean.TRUE, output.get("enabled"));
    }

    @Test
    public void yamlRejectsDuplicateKeys() {
        try {
            YamlHelper.loadMap("value: 1\nvalue: 2\n");
            fail("Duplicate YAML keys must be rejected");
        } catch (RuntimeException expected) {
            // SnakeYAML throws a marked YAML exception for the duplicate key.
        }
    }

    @Test
    public void classGraphFindsToolingPackage() {
        List<String> classes = ClassGraphHelper.allClasses("me.tamkungz.nilloadersdk.tooling");
        assertTrue(classes.contains(DeveloperToolbox.class.getName()));
    }

    @Test
    public void byteBuddyCanGenerateSimpleSubclass() {
        Class<? extends SampleBase> generated = ByteBuddyHelper.makeSubclass(SampleBase.class);
        assertTrue(SampleBase.class.isAssignableFrom(generated));
    }

    @Test
    public void gebBusCanBeCreatedWithoutDispatchers() {
        assertNotNull(GebHelper.createBus());
    }
}
