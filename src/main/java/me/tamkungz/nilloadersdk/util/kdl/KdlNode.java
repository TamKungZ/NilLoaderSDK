package me.tamkungz.nilloadersdk.util.kdl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class KdlNode {
    private final String name;
    private final List<KdlValue> arguments;
    private final Map<String, KdlValue> properties;
    private final List<KdlNode> children;

    public KdlNode(String name) {
        this.name = name;
        this.arguments = new ArrayList<KdlValue>();
        this.properties = new LinkedHashMap<String, KdlValue>();
        this.children = new ArrayList<KdlNode>();
    }

    public String getName() { return name; }
    public List<KdlValue> getArguments() { return arguments; }
    public Map<String, KdlValue> getProperties() { return properties; }
    public List<KdlNode> getChildren() { return children; }

    public void addArgument(KdlValue value) { arguments.add(value); }
    public void setProperty(String key, KdlValue value) { properties.put(key, value); }
    public void addChild(KdlNode child) { children.add(child); }
}

