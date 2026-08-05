package me.tamkungz.nilloadersdk.util.kdl;

import java.util.ArrayList;
import java.util.List;

public class KdlDocument {
    private final List<KdlNode> nodes;

    public KdlDocument() {
        this.nodes = new ArrayList<KdlNode>();
    }

    public List<KdlNode> getNodes() {
        return nodes;
    }

    public void addNode(KdlNode node) {
        nodes.add(node);
    }
}

