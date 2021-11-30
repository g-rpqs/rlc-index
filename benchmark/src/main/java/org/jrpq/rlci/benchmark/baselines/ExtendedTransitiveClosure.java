package org.jrpq.rlci.benchmark.baselines;

import org.jrpq.rlci.core.graphs.EdgeLabeledGraph;
import org.jrpq.rlci.core.kbs.*;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.IntIntImmutablePair;
import it.unimi.dsi.fastutil.ints.IntIntMutablePair;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.io.*;
import java.util.Iterator;
import java.util.Map;


public class ExtendedTransitiveClosure<V, E> implements Serializable{

    private static final long serialVersionUID = -442464187907079882L;
    private final IntIntPair INT_INT_PAIR;
    private final int k;
    private transient EdgeLabeledGraph<V, E> edgeLabeledGraph;
    private final Object2ObjectOpenHashMap<IntIntPair, ObjectOpenHashSet<MinimumRepeat>> tc;

    public static <V,E> void serialize(org.jrpq.rlci.benchmark.baselines.ExtendedTransitiveClosure<V,E> extendedTransitiveClosure, String output) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(output);
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
        objectOutputStream.writeObject(extendedTransitiveClosure);
        objectOutputStream.flush();
        objectOutputStream.close();
        fileOutputStream.close();
    }

    public static <V,E> org.jrpq.rlci.benchmark.baselines.ExtendedTransitiveClosure<V, E> deserialize(EdgeLabeledGraph<V,E> edgeLabeledGraph, String input) throws IOException, ClassNotFoundException {
        FileInputStream fileInputStream = new FileInputStream(input);
        ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
        org.jrpq.rlci.benchmark.baselines.ExtendedTransitiveClosure<V,E> extendedTransitiveClosure = (org.jrpq.rlci.benchmark.baselines.ExtendedTransitiveClosure<V, E>) objectInputStream.readObject();
        extendedTransitiveClosure.edgeLabeledGraph = edgeLabeledGraph;
        System.out.println("ETC for " + edgeLabeledGraph.getGraphName() + " has been loaded.");
        return extendedTransitiveClosure;
    }

    public ExtendedTransitiveClosure(EdgeLabeledGraph<V, E> graph, int k) {
        this.k = k;
        this.edgeLabeledGraph = graph;
        tc = new Object2ObjectOpenHashMap<>();
        INT_INT_PAIR = new IntIntMutablePair(0, 0);
    }

    public boolean query(V source, V target, String[] constraints) {
        MinimumRepeat labelConstraint = MinimumRepeat.computeMinimumRepeat(edgeLabeledGraph.encodeLabelConstraint(constraints));
        IntIntPair sourceAndTarget = INT_INT_PAIR.left(edgeLabeledGraph.getVertexId(source)).right(edgeLabeledGraph.getVertexId(target));
        ObjectOpenHashSet<MinimumRepeat> hashSet = tc.get(sourceAndTarget);
        return hashSet != null && hashSet.contains(labelConstraint);
    }

    public Object2ObjectOpenHashMap<IntIntPair, ObjectOpenHashSet<MinimumRepeat>> getTC() {
        return tc;
    }

    public void build() {
        System.out.println("start building TC for " + edgeLabeledGraph.getGraphName());
        LabelSequenceCollection labelSequenceCollection = LabelSequenceCollection.createInstance(edgeLabeledGraph.getLabelSet().size(), k);
        Object2ObjectOpenHashMap<MinimumRepeat, Pair<VertexIntQueue<V>, VertexMark>> kernelsAndVerticesWithStates = new Object2ObjectOpenHashMap<>();
        TCForwardKernelSearch kernelSearch = new TCForwardKernelSearch(k, edgeLabeledGraph, labelSequenceCollection);
        TCForwardKernelBfs kernelBfs = new TCForwardKernelBfs(edgeLabeledGraph);
        Iterator<V> vertices = edgeLabeledGraph.getVertices();
        while (vertices.hasNext()){
            V source = vertices.next();
            if (!Thread.currentThread().isInterrupted()){
                kernelSearch.prunedSearch(source, kernelsAndVerticesWithStates);
                for (Map.Entry<MinimumRepeat, Pair<VertexIntQueue<V>, VertexMark>> entry : kernelsAndVerticesWithStates.entrySet())
                    if (!entry.getValue().left().isEmpty())
                        kernelBfs.bfs(source, entry);
            } else
                break;
        }
        System.out.println("TC for " + edgeLabeledGraph.getGraphName() + " has been built.");
    }

    private boolean insertEntry(int sId, int vId, MinimumRepeat mr) {
        INT_INT_PAIR.left(sId).right(vId);
        ObjectOpenHashSet<MinimumRepeat> hashSet;
        if ((hashSet = tc.get(INT_INT_PAIR)) == null) {
            hashSet = new ObjectOpenHashSet<>();
            tc.put(new IntIntImmutablePair(sId, vId), hashSet);
        }
        return hashSet.add(mr);
    }

    private class TCForwardKernelSearch extends ForwardKernelSearch<V, E> {
        protected TCForwardKernelSearch(int k, EdgeLabeledGraph<V, E> edgeLabeledGraph, LabelSequenceCollection labelSequenceCollection) {
            super(k, edgeLabeledGraph, labelSequenceCollection);
        }

//        @Override
//        protected void insertWithConcatenation(int sId, int vId, MinimumRepeat mr, int repeat) {
//            insertEntry(sId, vId, mr);
//        }

        @Override
        protected boolean insert(int sId, int vId, MinimumRepeat mr, int repeat) {
            return insertEntry(sId, vId, mr);
        }
    }

    private class TCForwardKernelBfs extends ForwardKernelBfs<V, E> {

        protected TCForwardKernelBfs(EdgeLabeledGraph<V, E> edgeLabeledGraph) {
            super(edgeLabeledGraph);
        }

        @Override
        protected boolean insert(int sId, int vId, MinimumRepeat mr) {
            return insertEntry(sId, vId, mr);
        }

//        @Override
//        protected void insertWithConcatenation(int sId, int vId, MinimumRepeat mr) {
//            insertEntry(sId, vId, mr);
//        }
    }
}
