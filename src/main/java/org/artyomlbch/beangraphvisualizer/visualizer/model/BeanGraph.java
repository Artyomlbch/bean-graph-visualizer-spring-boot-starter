package org.artyomlbch.beangraphvisualizer.visualizer.model;

import org.springframework.context.annotation.Bean;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class BeanGraph {
    private final List<BeanNode> nodes;
    private final List<BeanNode> soloNodes;
    private final List<BeanEdge> edges;

    private BeanGraph(List<BeanNode> nodes, List<BeanNode> soloNodes, List<BeanEdge> edges) {
        this.nodes = List.copyOf(nodes);
        this.soloNodes = List.copyOf(soloNodes);
        this.edges = List.copyOf(edges);
    }

    public static BeanGraph of(Collection<BeanNode> allNodes, Collection<BeanEdge> edges) {
        Map<String, BeanNode> nodesById = new LinkedHashMap<>();
        for (BeanNode node : allNodes) {
            nodesById.putIfAbsent(node.id(), node);
        }

        Set<BeanEdge> keptEdges = new LinkedHashSet<>();
        Set<String> connectedIds = new HashSet<>();
        for (BeanEdge edge : edges) {
            String sourceId = edge.source().id();
            String targetId = edge.target().id();
            if (nodesById.containsKey(sourceId) &&  nodesById.containsKey(targetId)) {
                keptEdges.add(edge);
                connectedIds.add(sourceId);
                connectedIds.add(targetId);
            }
        }

        List<BeanNode> connected = new ArrayList<>();
        List<BeanNode> solo = new ArrayList<>();
        for (BeanNode node : nodesById.values()) {
            if (connectedIds.contains(node.id())) {
                connected.add(node);
            } else {
                solo.add(node);
            }
        }

        return new BeanGraph(connected, solo, new ArrayList<>(keptEdges));
    }

    public BeanGraph subgraph(Predicate<BeanNode> filter) {
        List<BeanNode> keptNodes = Stream.concat(nodes.stream(), soloNodes.stream())
                .filter(filter)
                .toList();

        return of(keptNodes, edges);
    }

    public List<BeanNode> getNodes() {
        return nodes;
    }

    public List<BeanNode> getSoloNodes() {
        return soloNodes;
    }

    public List<BeanEdge> getEdges() {
        return edges;
    }
}
