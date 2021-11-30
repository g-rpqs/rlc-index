package org.jrpq.rlci.core.graphs;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.jgrapht.graph.AbstractGraph;


import java.util.*;

public class JgtEdgeLabeledGraph implements EdgeLabeledGraph<Integer, RelationshipEdge> {
    AbstractGraph<Integer, RelationshipEdge> graph;

    Object2IntOpenHashMap<String> edgeLabelEncoder;

    String graphName;

    public JgtEdgeLabeledGraph(AbstractGraph<Integer, RelationshipEdge> graph, Object2IntOpenHashMap<String> edgeLabelEncoder) {
        this.graph = graph;
        this.edgeLabelEncoder = edgeLabelEncoder;
    }

    public JgtEdgeLabeledGraph(AbstractGraph<Integer, RelationshipEdge> graph, Object2IntOpenHashMap<String> edgeLabelEncoder, String graphName) {
        this.graph = graph;
        this.edgeLabelEncoder = edgeLabelEncoder;
        this.graphName = graphName;
    }

    @Override
    public int getEncodedEdgeLabel(RelationshipEdge edge) {
        return edge.getLabel();
    }

    @Override
    public int encodeEdgeLabel(String label) {
        return edgeLabelEncoder.getInt(label);
    }

    @Override
    public String decodeEdgeLabel(int encode) { // not an optimized version
        for (Object2IntMap.Entry<String> entry : edgeLabelEncoder.object2IntEntrySet()){
            if (entry.getIntValue() == encode)
                return entry.getKey();
        }
        return null;
//        return edgeLabelEncoder.object2IntEntrySet().stream().filter(stringEntry -> stringEntry.getIntValue() == encode).collect(Collectors.toList()).get(0).getKey();
    }

    @Override
    public int getNumberOfVertices() {
        return graph.vertexSet().size();
    }

    @Override
    public int getNumberOfEdges() {
        return graph.edgeSet().size();
    }

    @Override
    public Iterator<RelationshipEdge> outEdgesIterator(Integer integer) {
        return graph.outgoingEdgesOf(integer).iterator();
    }

    @Override
    public Iterator<RelationshipEdge> inEdgesIterator(Integer integer) {
        return graph.incomingEdgesOf(integer).iterator();
    }

    @Override
    public Integer getSourceOf(RelationshipEdge relationshipEdge) {
        return graph.getEdgeSource(relationshipEdge);
    }

    @Override
    public Integer getTargetOf(RelationshipEdge relationshipEdge) {
        return graph.getEdgeTarget(relationshipEdge);
    }

    @Override
    public int getVertexId(Integer integer) {
        return integer;
    }

    @Override
    public Iterator<Integer> getVertices() {
        return graph.vertexSet().iterator();
    }

    @Override
    public Iterator<RelationshipEdge> getEdges() {
        return graph.edgeSet().iterator();
    }

    @Override
    public Iterator<Integer> sortVerticesBasedOnDegree() {
//        return graph
//                .vertexSet()
//                .stream()
//                .sorted((o1, o2) -> (graph.inDegreeOf(o2) + 1) * (graph.outDegreeOf(o2) + 1) - (graph.inDegreeOf(o1) + 1) * (graph.outDegreeOf(o1) + 1))
//                .iterator();
        List<Integer> vList = new ArrayList<>(graph.vertexSet());
        vList.sort((o1, o2) -> {
            Integer integer1 = (graph.inDegreeOf(o1) + 1) * (graph.outDegreeOf(o1) + 1);
            Integer integer2 = (graph.inDegreeOf(o2) + 1) * (graph.outDegreeOf(o2) + 1);
            return integer2.compareTo(integer1);
        });
        System.out.println("Vertex list has been sorted");
        return vList.iterator();
    }

    @Override
    public Set<String> getLabelSet() {
        return edgeLabelEncoder.keySet();
    }

    @Override
    public Integer getVertex(int vId) {
        return vId;
    }

    @Override
    public String getGraphName() {
        return graphName;
    }
}
