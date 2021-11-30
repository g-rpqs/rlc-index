package org.jrpq.rlci.core.graphs;

import org.jrpq.rlci.core.RlcIndex;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.AbstractGraph;
import org.jgrapht.opt.graph.sparse.SparseIntDirectedGraph;

import java.util.*;

@Deprecated
public class EdgeLabeledJGraphT implements EdgeLabeledGraph<Integer, Integer> {

    AbstractGraph<Integer, Integer> graph;

    Int2IntOpenHashMap edgeToLabel;

    Object2IntOpenHashMap<String> edgeLabelEncoder;

    String graphName;

    public EdgeLabeledJGraphT(AbstractGraph<Integer, Integer> graph, Int2IntOpenHashMap edgeToLabel, Object2IntOpenHashMap<String> edgeLabelEncoder) {
        this.graph = graph;
        this.edgeToLabel = edgeToLabel;
        this.edgeLabelEncoder = edgeLabelEncoder;
    }

    public EdgeLabeledJGraphT(AbstractGraph<Integer, Integer> graph, Int2IntOpenHashMap edgeToLabel, Object2IntOpenHashMap<String> edgeLabelEncoder, String graphName) {
        this.graph = graph;
        this.edgeToLabel = edgeToLabel;
        this.edgeLabelEncoder = edgeLabelEncoder;
        this.graphName = graphName;
    }

    public static EdgeLabeledGraph<Integer, Integer> getASimpleInstance2() { // this method returns the example in Fig. 2 in the paper.
        List<Pair<Integer, Integer>> edgeList = new ArrayList<>();
        Int2IntOpenHashMap edge2Label = new Int2IntOpenHashMap();
        Object2IntOpenHashMap<String> edgeLabelEncoder = new Object2IntOpenHashMap<>();
        int[] labels = new int[]{1, 2, 3};
        for (int i = 1; i <= 3; i++)
            edgeLabelEncoder.put("l" + i, labels[i - 1]);
        int eId = 0;
        edgeList.add(Pair.of(1, 2)); edge2Label.put(eId++, edgeLabelEncoder.getInt("l1"));
        edgeList.add(Pair.of(2, 5)); edge2Label.put(eId++, edgeLabelEncoder.getInt("l1"));
        edgeList.add(Pair.of(1, 3)); edge2Label.put(eId++, edgeLabelEncoder.getInt("l2")); // change
        edgeList.add(Pair.of(3, 2)); edge2Label.put(eId++, edgeLabelEncoder.getInt("l1")); // change
        edgeList.add(Pair.of(3, 4)); edge2Label.put(eId++, edgeLabelEncoder.getInt("l2"));
        edgeList.add(Pair.of(4, 1)); edge2Label.put(eId++, edgeLabelEncoder.getInt("l1")); // change
        edgeList.add(Pair.of(4, 6)); edge2Label.put(eId++, edgeLabelEncoder.getInt("l3"));
        edgeList.add(Pair.of(5, 1)); edge2Label.put(eId++, edgeLabelEncoder.getInt("l1"));

        //new edge
        edgeList.add(Pair.of(3, 6)); edge2Label.put(eId++, edgeLabelEncoder.getInt("l1"));

        //new edge
        edgeList.add(Pair.of(3, 1)); edge2Label.put(eId++, edgeLabelEncoder.getInt("l2"));

        //new edge
        edgeList.add(Pair.of(2, 5)); edge2Label.put(eId, edgeLabelEncoder.getInt("l2"));


        SparseIntDirectedGraph sparseIntDirectedGraph = new SparseIntDirectedGraph(7, edgeList); // vid starts from 0, so there are 7 vertices
        return new EdgeLabeledJGraphT(sparseIntDirectedGraph, edge2Label, edgeLabelEncoder);
    }

    public static void main(String[] args) {
        RlcIndex<Integer, Integer> rlcIndex = new RlcIndex<>(getASimpleInstance2(), 2);
        rlcIndex.build();
        System.out.println(rlcIndex);
    }

    @Override
    public int getEncodedEdgeLabel(Integer edge) {
        return edgeToLabel.get(edge.intValue());
    }

    @Override
    public int getNumberOfVertices() {
        return graph.vertexSet().size();
    }

    @Override
    public int getNumberOfEdges() {
        return edgeToLabel.size();
    }

    @Override
    public int encodeEdgeLabel(String label) {
        return edgeLabelEncoder.getInt(label);
    }

    @Override
    public String decodeEdgeLabel(int encode) {
        for (Object2IntMap.Entry<String> stringEntry : edgeLabelEncoder.object2IntEntrySet()){
            if (stringEntry.getIntValue() == encode)
                return stringEntry.getKey();
        }
        return "null";
    }

    @Override
    public Iterator<Integer> outEdgesIterator(Integer v) {
        return graph.outgoingEdgesOf(v).iterator();
    }

    @Override
    public Iterator<Integer> inEdgesIterator(Integer v) {
        return graph.incomingEdgesOf(v).iterator();
    }

    @Override
    public Integer getSourceOf(Integer e) {
        return graph.getEdgeSource(e);
    }

    @Override
    public Integer getTargetOf(Integer e) {
        return graph.getEdgeTarget(e);
    }

    @Override
    public int getVertexId(Integer v) {
        return v;
    }

    @Override
    public Iterator<Integer> getVertices() {
        return graph.vertexSet().iterator();
    }

    @Override
    public Iterator<Integer> getEdges() {
        return graph.edgeSet().iterator();
    }

    @Override
    public Iterator<Integer> sortVerticesBasedOnDegree() {
        // Descending order of vertex degrees
        return graph
                .vertexSet()
                .stream()
//                .sorted((o1, o2) -> (graph.inDegreeOf(o2) + 1) * (graph.outDegreeOf(o2) + 1) - (graph.inDegreeOf(o1) + 1) * (graph.outDegreeOf(o1) + 1))
                .sorted((o1, o2) -> {
                    Integer integer1 = (graph.inDegreeOf(o1) + 1) * (graph.outDegreeOf(o1) + 1);
                    Integer integer2 = (graph.inDegreeOf(o2) + 1) * (graph.outDegreeOf(o2) + 1);
                    return integer2.compareTo(integer1);})
                .iterator();
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
