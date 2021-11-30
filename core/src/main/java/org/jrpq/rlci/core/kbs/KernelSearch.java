package org.jrpq.rlci.core.kbs;

import org.jrpq.rlci.core.graphs.EdgeLabeledGraph;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectObjectMutablePair;

import java.util.Iterator;

public abstract class KernelSearch<V, E> {
    protected final LabelSequenceCollection labelSequenceCollection;
    protected final EdgeLabeledGraph<V, E> edgeLabeledGraph;
    private final int k;
    private final VertexLabelSeqQueue<V> vertexLabelSeqQueue;

    protected KernelSearch(int k, EdgeLabeledGraph<V, E> edgeLabeledGraph, LabelSequenceCollection labelSequenceCollection) {
        this.k = k;
        this.edgeLabeledGraph = edgeLabeledGraph;
        this.vertexLabelSeqQueue = new VertexLabelSeqQueue<>();
        this.labelSequenceCollection = labelSequenceCollection;
    }

//    @Deprecated
//    protected abstract void insertWithConcatenation(int sId, int vId, MinimumRepeat mr, int repeat);

    // returns true if the index entry was not recorded otherwise false
    protected abstract boolean insert(int sId, int vId, MinimumRepeat mr, int repeat);

    protected abstract Iterator<E> nextEdges(V vertex);

    protected abstract V nextVertex(E edge);

    protected abstract LabelSequence nextLabelSeq(LabelSequence currentLabelSeq, E edge);

    protected abstract int computeStateId(int lenOfKernel);

    protected int computeId(V vertex) {
        return edgeLabeledGraph.getVertexId(vertex);
    }

//    //The version with concatenation of minimum repeats
//    @Deprecated
//    public void prunedSearchWithConcatenationOfMinimumRepeats(V source, Object2ObjectOpenHashMap<MinimumRepeat, Pair<VertexIntQueue<V>, VertexMark>> kernelsAndVerticesWithStates) {
//        int sId = computeId(source), vId; // the ID could be a vertex Id or its access Id
//        vertexLabelSeqQueue.enqueue(source, labelSequenceCollection.getEpsilon());
//        while (!vertexLabelSeqQueue.isEmpty()) {
//            Pair<V, LabelSequence> pair = vertexLabelSeqQueue.dequeue();
//            Iterator<E> edges = nextEdges(pair.left());
//            while (edges.hasNext()) {
//                E edge = edges.next();
//                V vertex = nextVertex(edge);
//                vId = computeId(vertex);
//                LabelSequence labelSequence = nextLabelSeq(pair.right(), edge);
//                if (labelSequenceCollection.isVisited(vId, labelSequence))
//                    continue;
//                MinimumRepeat mr = labelSequenceCollection.getMr(labelSequence);
//                insertWithConcatenation(sId, vId, mr, labelSequence.length() / mr.length());
//                Pair<VertexIntQueue<V>, VertexMark> frontiers = kernelsAndVerticesWithStates
//                        .computeIfAbsent(mr, k -> new ObjectObjectMutablePair<>(new VertexIntQueue<>(), null));
//                if (frontiers.right() == null)
//                    frontiers.right(new HashMapVertexMark(mr.length()));
//                int stateId = computeStateId(mr.length());
//                if (!frontiers.right().markIfNotVisited(vId, stateId))
//                    frontiers.left().enqueue(vertex, stateId);
//                if (labelSequence.length() < k)
//                    vertexLabelSeqQueue.enqueue(vertex, labelSequence);
//            }
//        }
//        labelSequenceCollection.clearMarks();
//    }

    //The version without concatenation of minimum repeats
    public void prunedSearch(V source, Object2ObjectOpenHashMap<MinimumRepeat, Pair<VertexIntQueue<V>, VertexMark>> kernelsAndVerticesWithStates) {
        int sId = computeId(source); // the ID could be a vertex Id or its access Id
        vertexLabelSeqQueue.enqueue(source, labelSequenceCollection.getEpsilon());
        while (!vertexLabelSeqQueue.isEmpty()) {
            Pair<V, LabelSequence> pair = vertexLabelSeqQueue.dequeue();
            Iterator<E> edges = nextEdges(pair.left());
            while (edges.hasNext()) {
                E edge = edges.next();
                V vertex = nextVertex(edge);
                int vId = computeId(vertex);
                LabelSequence labelSequence = nextLabelSeq(pair.right(), edge);
                if (labelSequenceCollection.isVisited(vId, labelSequence))
                    continue;
                MinimumRepeat mr = labelSequenceCollection.getMr(labelSequence);
                // The following implementation is a slightly better version than the one at line 14 in Algorithm 2 of the paper.
                // The idea is to prune more cases:
                //  Case 1: if mr.length() != labelSequence.length(), then the reachability information can be covered by kernel-bfs performed later.
                //          Therefore, we do not need to record the frontier vertex twice
                //  Case 2: if insert(sId, vId, mr) returns false, then the reachability information can be recovered by the current snapshot of the index.
                //          Therefore, a traversal that has mr.length() depth can be saved.
                if (mr.length() == labelSequence.length() && insert(sId, vId, mr, labelSequence.length() / mr.length())) {
                    Pair<VertexIntQueue<V>, VertexMark> frontiers = kernelsAndVerticesWithStates
                            .computeIfAbsent(mr, k -> new ObjectObjectMutablePair<>(new VertexIntQueue<>(), null));
                    if (frontiers.right() == null)
                        frontiers.right(new HashMapVertexMark(mr.length()));
                    int stateId = computeStateId(mr.length());
                    if (!frontiers.right().markIfNotVisited(vId, stateId))
                        frontiers.left().enqueue(vertex, stateId);
                }
                if (labelSequence.length() < k)
                    vertexLabelSeqQueue.enqueue(vertex, labelSequence);
            }
        }
        labelSequenceCollection.clearMarks();
    }
}