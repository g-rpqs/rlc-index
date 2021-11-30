package org.jrpq.rlci.core;

import org.jrpq.rlci.core.graphs.EdgeLabeledGraph;
import org.jrpq.rlci.core.internal.TableIndexArrayImpl;
import org.jrpq.rlci.core.internal.TableIndex;
import org.jrpq.rlci.core.kbs.*;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectObjectMutablePair;

import java.io.*;
import java.util.*;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

public class RlcIndex<V, E> implements Serializable {

    private static final long serialVersionUID = -6944383255757819085L;
    private final TableIndex tableIndex;
    private final int k;
    private final int[] vId2AccessId;
    private final V[] accessId2V;
    private transient EdgeLabeledGraph<V, E> edgeLabeledGraph;
    private boolean hasBeenBuilt = false;

    @SuppressWarnings("unchecked")
    public RlcIndex(EdgeLabeledGraph<V, E> edgeLabeledGraph, int k) {
        this.edgeLabeledGraph = edgeLabeledGraph;
        this.k = k;
        int numOfVertices = edgeLabeledGraph.getNumberOfVertices();
        tableIndex = new TableIndexArrayImpl(numOfVertices);
        vId2AccessId = new int[numOfVertices]; // assuming vertex ID starts from 0
        accessId2V = (V[]) new Object[numOfVertices];
    }

    public static <V, E> void serialize(RlcIndex<V, E> rlcIndex, String output) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(output);
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
        objectOutputStream.writeObject(rlcIndex);
        objectOutputStream.flush();
        objectOutputStream.close();
        fileOutputStream.close();
    }

    public static <V, E> RlcIndex<V, E> deserialize(EdgeLabeledGraph<V, E> edgeLabeledGraph, String input) throws IOException, ClassNotFoundException {
        FileInputStream fileInputStream = new FileInputStream(input);
        ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
        RlcIndex<V, E> rlcIndex = (RlcIndex) objectInputStream.readObject();
        rlcIndex.edgeLabeledGraph = edgeLabeledGraph;
        System.out.println("The RLC index for " + edgeLabeledGraph.getGraphName() + " has been loaded.");
        return rlcIndex;
    }

    public boolean query(V source, V target, String[] constraints) {
        if (constraints.length > k) {
            System.out.println("The query cannot be supported by the RLC index.");
            System.out.println("Current k: " + k + ", needed k: " + constraints.length);
            return false;
        }
        MinimumRepeat minimumRepeat = MinimumRepeat.computeMinimumRepeat(edgeLabeledGraph.encodeLabelConstraint(constraints));
        if (minimumRepeat.length() != constraints.length){
            System.out.println("Invalid constraint");
            return false;
        }
        return tableIndex.queryWithoutConcatenation(getAccessIdOf(source), getAccessIdOf(target), minimumRepeat);
    }

    boolean queryWithVertexId(int sId, int tId, MinimumRepeat minimumRepeat) {
        return tableIndex.queryWithoutConcatenation(vId2AccessId[sId], vId2AccessId[tId], minimumRepeat);
    }

    boolean queryWithAccessId(int sId, int tId, MinimumRepeat minimumRepeat) {
        return tableIndex.queryWithoutConcatenation(sId, tId, minimumRepeat);
    }

    int getAccessIdOf(V v) {
        return vId2AccessId[edgeLabeledGraph.getVertexId(v)];
    }

    V getVOfAccessId(int accessId) {
        return accessId2V[accessId];
    }

    private void init() {
        int accessId = 0;
        Iterator<V> vertices = edgeLabeledGraph.sortVerticesBasedOnDegree();
        while (vertices.hasNext()) {
            V v = vertices.next();
            vId2AccessId[edgeLabeledGraph.getVertexId(v)] = accessId;
            accessId2V[accessId++] = v;
        }
    }

    public void build() {
        if (hasBeenBuilt)
            throw new UnsupportedOperationException("The RLC index for " + edgeLabeledGraph.getGraphName() + " has been built.");
        int accessId = 0;
        Iterator<V> vertices = edgeLabeledGraph.sortVerticesBasedOnDegree();
        while (vertices.hasNext()) {
            V v = vertices.next();
            vId2AccessId[edgeLabeledGraph.getVertexId(v)] = accessId;
            accessId2V[accessId++] = v;
        }
        System.out.println("Start building the RLC index for " + edgeLabeledGraph.getGraphName());
        LabelSequenceCollection labelSequenceCollection = LabelSequenceCollection.createInstance(edgeLabeledGraph.getLabelSet().size(), k);
        Object2ObjectOpenHashMap<MinimumRepeat, Pair<VertexIntQueue<V>, VertexMark>> kernelsAndVerticesWithStates = new Object2ObjectOpenHashMap<>();
        KernelSearch<V, E> forwardKernelSearch = new RlcForwardKernelSearch(k, edgeLabeledGraph, labelSequenceCollection), backwardKernelSearch = new RlcBackwardKernelSearch(k, edgeLabeledGraph, labelSequenceCollection);
        KernelBfs<V, E> forwardKernelBfs = new RlcForwardKernelBfs(edgeLabeledGraph), backwardKernelBfs = new RlcBackwardKernelBfs(edgeLabeledGraph);
        int i = 0;
        for (V source : accessId2V) {
            if (!Thread.currentThread().isInterrupted())
                search(source, backwardKernelSearch, backwardKernelBfs, kernelsAndVerticesWithStates);
            else
                break;
            if (!Thread.currentThread().isInterrupted())
                search(source, forwardKernelSearch, forwardKernelBfs, kernelsAndVerticesWithStates);
            else
                break;
            i++;
            if (i % 500_000 == 0)
                System.out.println(i);
        }
        System.out.println("The RLC index for " + edgeLabeledGraph.getGraphName() + " has been built.");
        hasBeenBuilt = true;
    }

    public void build(String[] recursiveLabels) {
        if (hasBeenBuilt)
            throw new UnsupportedOperationException("The RLC index for " + edgeLabeledGraph.getGraphName() + " has been built.");
        init();
        System.out.println("Start building the RLC index for " + edgeLabeledGraph.getGraphName());
        buildWithKernelCandidates(getAllPossibleKernels(recursiveLabels));
        System.out.println("The RLC index for " + edgeLabeledGraph.getGraphName() + " has been built.");
        hasBeenBuilt = true;
    }

    public void build(Set<String[]> kernelCandidates) {
        if (hasBeenBuilt)
            throw new UnsupportedOperationException("The RLC index for " + edgeLabeledGraph.getGraphName() + " has been built.");
        init();
        Set<MinimumRepeat> kernels = kernelCandidates
                .stream()
                .map(cons -> {
                    int[] encodeCons = edgeLabeledGraph.encodeLabelConstraint(cons);
                    MinimumRepeat mr = MinimumRepeat.computeMinimumRepeat(encodeCons);
                    return mr.length() == encodeCons.length ? mr : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        System.out.println("Start building the RLC index for " + edgeLabeledGraph.getGraphName());
        buildWithKernelCandidates(kernels);
        System.out.println("The RLC index for " + edgeLabeledGraph.getGraphName() + " has been built.");
        hasBeenBuilt = true;
    }

    private void buildWithKernelCandidates(Set<MinimumRepeat> kernels) {
        Object2ObjectOpenHashMap<MinimumRepeat, Pair<VertexIntQueue<V>, VertexMark>> kernelsAndVerticesWithStates = new Object2ObjectOpenHashMap<>();
        for (MinimumRepeat kernel : kernels)
            kernelsAndVerticesWithStates.put(kernel, ObjectObjectMutablePair.of(new VertexIntQueue<>(), null));
        KernelBfs<V, E> forwardKernelBfs = new RlcForwardKernelBfs(edgeLabeledGraph), backwardKernelBfs = new RlcBackwardKernelBfs(edgeLabeledGraph);
        ToIntFunction<MinimumRepeat> getInitialStateIdForBbfs = (mr) -> 0, getInitialStateIdForFbfs = (mr) -> mr.length() - 1;
        for (V source : accessId2V) {
            if (!Thread.currentThread().isInterrupted())
                search(source, backwardKernelBfs, kernelsAndVerticesWithStates, getInitialStateIdForBbfs);
            else
                break;
            if (!Thread.currentThread().isInterrupted())
                search(source, forwardKernelBfs, kernelsAndVerticesWithStates, getInitialStateIdForFbfs);
            else
                break;
        }
    }

    private Set<MinimumRepeat> getAllPossibleKernels(String[] recursiveLabels) {
        int[] labels = new int[recursiveLabels.length];
        for (int i = 0; i < recursiveLabels.length; i++)
            labels[i] = edgeLabeledGraph.encodeEdgeLabel(recursiveLabels[i]);
        List<List<int[]>> list = new ArrayList<>();
        Set<MinimumRepeat> minimumRepeats = new HashSet<>();
        for (int i = 0; i < k; i++) {
            List<int[]> lenI = new ArrayList<>();
            list.add(lenI);
            if (i > 0) {
                for (int[] PreCons : list.get(i - 1)) {
                    for (int l : labels) {
                        int[] newCons = new int[PreCons.length + 1];
                        System.arraycopy(PreCons, 0, newCons, 0, PreCons.length);
                        newCons[newCons.length - 1] = l;
                        lenI.add(newCons);
                        MinimumRepeat temp = MinimumRepeat.computeMinimumRepeat(newCons);
                        if (temp.length() == newCons.length)
                            minimumRepeats.add(temp);
                    }
                }
            } else { // i = 0
                for (int l : labels) {
                    int[] newCons = new int[]{l};
                    lenI.add(newCons);
                    minimumRepeats.add(MinimumRepeat.computeMinimumRepeat(newCons));
                }
            }
        }
        return minimumRepeats;
    }

    private void search(V source, KernelBfs<V, E> kernelBFS, Object2ObjectOpenHashMap<MinimumRepeat, Pair<VertexIntQueue<V>, VertexMark>> kernelsAndVerticesWithStates, ToIntFunction<MinimumRepeat> initStateId) {
        for (Map.Entry<MinimumRepeat, Pair<VertexIntQueue<V>, VertexMark>> entry : kernelsAndVerticesWithStates.entrySet()) {
            entry.getValue().right(new HashMapVertexMark(entry.getKey().length()));
            entry.getValue().left().enqueue(source, initStateId.applyAsInt(entry.getKey()));
            kernelBFS.bfs(source, entry);
        }
    }

    private void search(V source, KernelSearch<V, E> kernelSearch, KernelBfs<V, E> kernelBFS, Object2ObjectOpenHashMap<MinimumRepeat, Pair<VertexIntQueue<V>, VertexMark>> kernelsAndVerticesWithStates) {
        kernelSearch.prunedSearch(source, kernelsAndVerticesWithStates);
        for (Map.Entry<MinimumRepeat, Pair<VertexIntQueue<V>, VertexMark>> entry : kernelsAndVerticesWithStates.entrySet())
            if (!entry.getValue().left().isEmpty())
                kernelBFS.bfs(source, entry);
    }

    public TableIndex getIndex() {
        return tableIndex;
    }

    public EdgeLabeledGraph<V, E> getEdgeLabeledGraph() {
        return edgeLabeledGraph;
    }

    public int getK() {
        return k;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        List<org.jgrapht.alg.util.Pair<List<IntObjectPair<MinimumRepeat>>, List<IntObjectPair<MinimumRepeat>>>> indexView = tableIndex.getIndexView();
        int i = 0;

        for (org.jgrapht.alg.util.Pair<List<IntObjectPair<MinimumRepeat>>, List<IntObjectPair<MinimumRepeat>>> listListPair : indexView) {
            stringBuilder.append("V = ").append(accessId2V[i++]).append(": ");
            stringBuilder.append("LIN = {");
            listListPair.getFirst().forEach(pair -> {
                if (pair == null)
                    stringBuilder.append("null");
                else
                    stringBuilder.append("(").append(accessId2V[pair.firstInt()]).append(", ").append(edgeLabeledGraph.decodeLabelConstraint(pair.second().getMrView())).append(") ");
            });
            stringBuilder.append("}, ");
            stringBuilder.append("LOUT = {");
            listListPair.getSecond().forEach(pair -> {
                if (pair == null)
                    stringBuilder.append("null");
                else
                    stringBuilder.append("(").append(accessId2V[pair.firstInt()]).append(", ").append(edgeLabeledGraph.decodeLabelConstraint(pair.second().getMrView())).append(") ");
            });
            stringBuilder.append("}").append("\n");
        }
        return stringBuilder.toString();
    }

    // The path is vId -> sId
    private boolean bBfsInsert(int sId, int vId, MinimumRepeat mr, boolean isT) {
        if (sId > vId)
            return false;
        else {
            boolean ret = tableIndex.backwardQuery(vId, sId, mr);
            if (!ret)
                tableIndex.addOutEntry(vId, sId, mr, isT);
            return !ret;
        }
    }

    // The path is sId -> vId
    private boolean fBfsInsert(int sId, int vId, MinimumRepeat mr, boolean isT) {
        if (sId > vId)
            return false;
        else {
            boolean ret = tableIndex.forwardQuery(sId, vId, mr);
            if (!ret)
                tableIndex.addInEntry(vId, sId, mr, isT);
            return !ret;
        }
    }

//    @Deprecated
//    public void buildWithConcatenationOfMinimumRepeats() {
//        int accessId = 0;
//        Iterator<V> vertices = edgeLabeledGraph.sortVerticesBasedOnDegree();
//        while (vertices.hasNext()) {
//            V v = vertices.next();
//            vId2AccessId[edgeLabeledGraph.getVertexId(v)] = accessId;
//            accessId2V[accessId++] = v;
//        }
//        System.out.println("start building");
//        LabelSequenceCollection labelSequenceCollection = LabelSequenceCollection.createInstance(edgeLabeledGraph.getLabelSet().size(), k);
//        Object2ObjectOpenHashMap<MinimumRepeat, Pair<VertexIntQueue<V>, VertexMark>> kernelsAndVerticesWithStates = new Object2ObjectOpenHashMap<>();
//        KernelSearch<V, E> forwardKernelSearch = new RlcForwardKernelSearch(k, edgeLabeledGraph, labelSequenceCollection), backwardKernelSearch = new RlcBackwardKernelSearch(k, edgeLabeledGraph, labelSequenceCollection);
//        KernelBfs<V, E> forwardKernelBfs = new RlcForwardKernelBfs(edgeLabeledGraph), backwardKernelBfs = new RlcBackwardKernelBfs(edgeLabeledGraph);
//        for (V source : accessId2V) {
//            if (!Thread.currentThread().isInterrupted())
//                perform2KSearchWithConcatenation(source, backwardKernelSearch, backwardKernelBfs, kernelsAndVerticesWithStates);
//            else
//                break;
//            if (!Thread.currentThread().isInterrupted())
//                perform2KSearchWithConcatenation(source, forwardKernelSearch, forwardKernelBfs, kernelsAndVerticesWithStates);
//            else
//                break;
//        }
//        System.out.println("end of building");
//    }

//    @Deprecated
//    private void perform2KSearchWithConcatenation(V source, KernelSearch<V, E> kernelSearch, KernelBfs<V, E> kernelBFS, Object2ObjectOpenHashMap<MinimumRepeat, Pair<VertexIntQueue<V>, VertexMark>> kernelsAndVerticesWithStates) {
//        kernelSearch.prunedSearchWithConcatenationOfMinimumRepeats(source, kernelsAndVerticesWithStates);
//        for (Map.Entry<MinimumRepeat, Pair<VertexIntQueue<V>, VertexMark>> entry : kernelsAndVerticesWithStates.entrySet())
//            if (!entry.getValue().left().isEmpty())
//                kernelBFS.bfsWithConcatenationOfMinimumRepeats(source, entry);
//    }
//
//    @Deprecated
//    protected boolean isPruned(int sId, int vId, int lenOfKernel) {
//        return vId < sId && lenOfKernel == 1;
//    }

//    // This insert function is used only in case of query with concatenation
//    @Deprecated
//    private void tryToInsertInBBfs(int sId, int vId, MinimumRepeat mr, boolean isT) {
//        if (!(sId > vId || tableIndex.queryWithConcatenation(vId, sId, mr)))
//            tableIndex.addOutEntry(vId, sId, mr, isT);
//    }
//
//    // Used in case of query with concatenation
//    @Deprecated
//    private void tryToInsertInFBfs(int sId, int vId, MinimumRepeat mr, boolean isT) {
//        if (!(sId > vId || tableIndex.queryWithConcatenation(sId, vId, mr)))
//            tableIndex.addInEntry(vId, sId, mr, isT);
//    }

    private class RlcForwardKernelSearch extends ForwardKernelSearch<V, E> {

        protected RlcForwardKernelSearch(int k, EdgeLabeledGraph<V, E> edgeLabeledGraph, LabelSequenceCollection labelSequenceCollection) {
            super(k, edgeLabeledGraph, labelSequenceCollection);
        }

//        @Override
//        protected void insertWithConcatenation(int sId, int vId, MinimumRepeat mr, int repeat) {
//            tryToInsertInFBfs(sId, vId, mr, repeat == 1);
//        }

        @Override
        protected boolean insert(int sId, int vId, MinimumRepeat mr, int repeat) {
            return fBfsInsert(sId, vId, mr, repeat == 1);
        }

        @Override
        protected int computeId(V vertex) {
            return vId2AccessId[edgeLabeledGraph.getVertexId(vertex)];
        }
    }

    private class RlcBackwardKernelSearch extends BackwardKernelSearch<V, E> {

        protected RlcBackwardKernelSearch(int k, EdgeLabeledGraph<V, E> edgeLabeledGraph, LabelSequenceCollection labelSequenceCollection) {
            super(k, edgeLabeledGraph, labelSequenceCollection);
        }

//        @Override
//        protected void insertWithConcatenation(int sId, int vId, MinimumRepeat mr, int repeat) {
//            tryToInsertInBBfs(sId, vId, mr, repeat == 1);
//        }

        @Override
        protected boolean insert(int sId, int vId, MinimumRepeat mr, int repeat) {
            return bBfsInsert(sId, vId, mr, repeat == 1);
        }

        @Override
        protected int computeId(V vertex) {
            return vId2AccessId[edgeLabeledGraph.getVertexId(vertex)];
        }
    }

    private class RlcForwardKernelBfs extends ForwardKernelBfs<V, E> {

        protected RlcForwardKernelBfs(EdgeLabeledGraph<V, E> edgeLabeledGraph) {
            super(edgeLabeledGraph);
        }

        @Override
        protected boolean insert(int sId, int vId, MinimumRepeat mr) {
            return fBfsInsert(sId, vId, mr, false);
        }

//        @Override
//        protected void insertWithConcatenation(int sId, int vId, MinimumRepeat mr) {
//            tryToInsertInFBfs(sId, vId, mr, false);
//        }

        @Override
        protected int computeId(V vertex) {
            return vId2AccessId[edgeLabeledGraph.getVertexId(vertex)];
        }

//        @Deprecated
//        @Override
//        protected boolean isPruned(int sId, int vId, int lenOfKernel) {
//            return RlcIndex.this.isPruned(sId, vId, lenOfKernel);
//        }
    }

    private class RlcBackwardKernelBfs extends BackwardKernelBfs<V, E> {

        protected RlcBackwardKernelBfs(EdgeLabeledGraph<V, E> edgeLabeledGraph) {
            super(edgeLabeledGraph);
        }

        @Override
        protected boolean insert(int sId, int vId, MinimumRepeat mr) {
            return bBfsInsert(sId, vId, mr, false);
        }

//        @Override
//        protected void insertWithConcatenation(int sId, int vId, MinimumRepeat mr) {
//            tryToInsertInBBfs(sId, vId, mr, false);
//        }

        @Override
        protected int computeId(V vertex) {
            return vId2AccessId[edgeLabeledGraph.getVertexId(vertex)];
        }

//        @Deprecated
//        @Override
//        protected boolean isPruned(int sId, int vId, int lenOfKernel) {
//            return RlcIndex.this.isPruned(sId, vId, lenOfKernel);
//        }
    }

}
