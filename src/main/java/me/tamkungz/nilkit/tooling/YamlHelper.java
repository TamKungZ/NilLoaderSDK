package me.tamkungz.nilkit.tooling;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Safe-by-default YAML convenience methods backed by SnakeYAML.
 *
 * <p>The loader uses {@link SafeConstructor}, rejects duplicate mapping keys,
 * and limits aliases. It intentionally does not deserialize arbitrary Java
 * object tags.</p>
 */
public final class YamlHelper {

    private YamlHelper() {
    }

    public static Object load(String text) {
        if (text == null) throw new IllegalArgumentException("text must not be null");
        return yaml().load(text);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> loadMap(String text) {
        Object value = load(text);
        if (value == null) return Collections.emptyMap();
        if (!(value instanceof Map)) {
            throw new IllegalArgumentException("YAML root must be a mapping");
        }
        return Collections.unmodifiableMap(new LinkedHashMap<String, Object>((Map<String, Object>) value));
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> loadMap(Path path) throws IOException {
        if (path == null) throw new IllegalArgumentException("path must not be null");
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Object value = yaml().load(reader);
            if (value == null) return Collections.emptyMap();
            if (!(value instanceof Map)) {
                throw new IllegalArgumentException("YAML root must be a mapping: " + path);
            }
            return Collections.unmodifiableMap(new LinkedHashMap<String, Object>((Map<String, Object>) value));
        }
    }

    public static String dump(Object value) {
        return yaml().dump(value);
    }

    public static void save(Path path, Object value) throws IOException {
        if (path == null) throw new IllegalArgumentException("path must not be null");
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            yaml().dump(value, writer);
        }
    }

    /** Creates an independent SnakeYAML instance with conservative loader options. */
    public static Yaml yaml() {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        options.setMaxAliasesForCollections(50);
        return new Yaml(new SafeConstructor(options));
    }
}
