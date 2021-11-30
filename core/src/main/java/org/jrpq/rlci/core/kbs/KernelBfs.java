package org.jrpq.rlci.core.kbs;

import org.jrpq.rlci.core.graphs.EdgeLabeledGraph;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectIntPair;

import java.util.Iterator;
import java.util.Map;

public abstract class KernelBfs<V, E> {
    protected final EdgeLabeledGraph<V, E> edgeLabeledGraph;
    protected int firstState, lastState, triggerState, increment;

    protected KernelBfs(EdgeLabeledGraph<V, E> edgeLabeledGraph, int increment) {
        this.edgeLabeledGraph = edgeLabeledGraph;
        this.increment = increment;
    }

    // returns true if the index entry (sId, vId, mr) is not contained otherwise false
    protected abstract boolean insert(int sId, int vId, MinimumRepeat mr);

//    protected abstract void insertWithConcatenation(int sId, int vId, MinimumRepeat mr);

    protected abstract Iterator<E> nextEdges(V vertex);

    protected abstract V nextVertex(E edge);

    protected abstract int firstState(int lenOfKernel);

    protected abstract int lastState(int lenOfKernel);

    protected int computeId(V vertex) {
        return edgeLabeledGraph.getVertexId(vertex);
    }

    @Deprecated
    protected boolean isPruned(int sId, int vId, int lenOfKernel) {
        return false;
    }

    private void setUpStates(int lengthOfKernel) {
        this.firstState = firstState(lengthOfKernel);
        this.lastState = lastState(lengthOfKernel);
        this.triggerState = lastState + increment;
    }

//    @Deprecated
//    public void bfsWithConcatenationOfMinimumRepeats(V source, Map.Entry<MinimumRepeat, Pair<VertexIntQueue<V>, VertexMark>> entry) {
//        int sId = computeId(source);
//        MinimumRepeat kernel = entry.getKey();
//        VertexIntQueue<V> vertexIntQueue = entry.getValue().left();
//        VertexMark vertexMark = entry.getValue().right();
//        entry.getValue().right(null); // VertexMark is not reused
//        setUpStates(kernel.length());
//        while (!vertexIntQueue.isEmpty()) {
//            ObjectIntPair<V> pair = vertexIntQueue.dequeue();
//            V v = pair.left();
//            int nextStateId = pair.rightInt() + increment;
//            if (nextStateId == triggerState)
//                nextStateId = firstState;
//            int label = kernel.getState(nextStateId);
//            Iterator<E> edges = nextEdges(v);
//            while (edges.hasNext()) {
//                E edge = edges.next();
//                if (edgeLabeledGraph.getEncodedEdgeLabel(edge) != label)
//                    continue;
//                V u = nextVertex(edge);
//                int uId = computeId(u);
//                if (nextStateId == lastState) {
//                    if (isPruned(sId, uId, kernel.length()))
//                        continue;
//                    insertWithConcatenation(sId, uId, kernel);
//                }
//                if (!vertexMark.markIfNotVisited(uId, nextStateId)) {  // markIfNotVisited(uId, stateId) returns true if u has been visited by the state kernel[stateId]
//                    vertexIntQueue.enqueue(u, nextStateId);
//                }
//            }
//        }
//    }

    public void bfs(V source, Map.Entry<MinimumRepeat, Pair<VertexIntQueue<V>, VertexMark>> entry) {
        int sId = computeId(source);
        MinimumRepeat kernel = entry.getKey();
        VertexIntQueue<V> vertexIntQueue = entry.getValue().left();
        VertexMark vertexMark = entry.getValue().right();
        entry.getValue().right(null); // VertexMark is not reused
        setUpStates(kernel.length());
        while (!vertexIntQueue.isEmpty()) {
            ObjectIntPair<V> pair = vertexIntQueue.dequeue();
            V v = pair.first();
            int nextStateId = pair.rightInt() + increment;
            if (nextStateId == triggerState)
                nextStateId = firstState;
            int label = kernel.getState(nextStateId);
            Iterator<E> edges = nextEdges(v);
            while (edges.hasNext()) {
                E edge = edges.next();
                if (edgeLabeledGraph.getEncodedEdgeLabel(edge) != label)
                    continue;
                V u = nextVertex(edge);
                int uId = computeId(u);
                if (!vertexMark.markIfNotVisited(uId, nextStateId) && (nextStateId != lastState || insert(sId, uId, kernel))) // if uid == sid, then the source is visited only once
                    vertexIntQueue.enqueue(u, nextStateId);
            }
        }
    }
}
