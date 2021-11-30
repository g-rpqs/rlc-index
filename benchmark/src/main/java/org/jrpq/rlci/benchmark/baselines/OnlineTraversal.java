package org.jrpq.rlci.benchmark.baselines;

import org.jrpq.rlci.benchmark.util.generators.Query;
import org.jrpq.rlci.core.graphs.EdgeLabeledGraph;
import org.jrpq.rlci.core.kbs.HashMapVertexMark;
import org.jrpq.rlci.core.kbs.VertexIntQueue;
import org.jrpq.rlci.core.kbs.VertexMark;
import it.unimi.dsi.fastutil.objects.ObjectIntPair;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Function;

public final class OnlineTraversal {
    public static <V, E> boolean reachBfs(EdgeLabeledGraph<V, E> edgeLabeledGraph, V source, V target, String[] constraints) {
        int[] cons = edgeLabeledGraph.encodeLabelConstraint(constraints);
        VertexMark vertexMark = new HashMapVertexMark(cons.length);
        VertexIntQueue<V> queue = new VertexIntQueue<>();
        queue.enqueue(source, 0);
        while (!queue.isEmpty()) {
            ObjectIntPair<V> pair = queue.dequeue();
            V vertex = pair.left();
            int stateId = pair.rightInt();
            if (stateId == cons.length) {
                if (vertex.equals(target))
                    return true;
                stateId = 0;
            }
            Iterator<E> outEdges = edgeLabeledGraph.outEdgesIterator(vertex);
            while (outEdges.hasNext()) {
                E edge = outEdges.next();
                V u = edgeLabeledGraph.getTargetOf(edge);
                if (edgeLabeledGraph.getEncodedEdgeLabel(edge) != cons[stateId] || vertexMark.markIfNotVisited(edgeLabeledGraph.getVertexId(u), stateId))
                    continue;
                queue.enqueue(u, stateId + 1);
            }
        }
        return false;
    }

    public static <V, E> boolean reachBiBfs(EdgeLabeledGraph<V, E> edgeLabeledGraph, V source, V target, String[] constraints) {
        int[] cons = edgeLabeledGraph.encodeLabelConstraint(constraints);
        // it is not correct to mark source and target in forwardVisited and backwardVisited because they could be same, which leads to false positive results
        VertexMark forwardVisited = new HashMapVertexMark(cons.length), backwardVisited = new HashMapVertexMark(cons.length);
        VertexIntQueue<V> forwardQueue = new VertexIntQueue<>(), backwardQueue = new VertexIntQueue<>();
        forwardQueue.enqueue(source, 0);
        backwardQueue.enqueue(target, cons.length - 1);
        while (!forwardQueue.isEmpty() && !backwardQueue.isEmpty()) {
            // forward start
            ObjectIntPair<V> fPair = forwardQueue.dequeue();
            int forwardStateId = fPair.rightInt();
            if (forwardStateId == cons.length) {
                forwardStateId = 0;
            }
            // check whether forward-vertex is visited by the backward search with the forward-state id
            if (backwardVisited.isVisited(edgeLabeledGraph.getVertexId(fPair.left()), forwardStateId))
                return true;
            // forward start
            Iterator<E> outEdges = edgeLabeledGraph.outEdgesIterator(fPair.left());
            while (outEdges.hasNext()) {
                E edge = outEdges.next();
                V u = edgeLabeledGraph.getTargetOf(edge);
                if (edgeLabeledGraph.getEncodedEdgeLabel(edge) != cons[forwardStateId] || forwardVisited.markIfNotVisited(edgeLabeledGraph.getVertexId(u), forwardStateId))
                    continue;
                forwardQueue.enqueue(u, forwardStateId + 1);
            }
            // forward end
            // ******************
            // backward start
            ObjectIntPair<V> bPair = backwardQueue.dequeue();
            int backwardStateId = bPair.rightInt();
            if (backwardStateId == -1)
                backwardStateId = cons.length - 1;
            // check whether backward-vertex is visited by the forward search with the backward-state id
            if (forwardVisited.isVisited(edgeLabeledGraph.getVertexId(bPair.left()), backwardStateId))
                return true;
            Iterator<E> inEdges = edgeLabeledGraph.inEdgesIterator(bPair.left());
            while (inEdges.hasNext()) {
                E edge = inEdges.next();
                V u = edgeLabeledGraph.getSourceOf(edge);
                if (edgeLabeledGraph.getEncodedEdgeLabel(edge) != cons[backwardStateId] || backwardVisited.markIfNotVisited(edgeLabeledGraph.getVertexId(u), backwardStateId))
                    continue;
                backwardQueue.enqueue(u, backwardStateId - 1);
            }
            // backward end
        }
        return false;
    }

    public static <V> void testCorrectness(Collection<Query<V>> queries, Function<Query<V>, Boolean> function) {
        queries.forEach(query -> {
                if (query.isTrue() != function.apply(query)) {
                    System.out.println("Wrong");
                    System.out.println(query);
                }
        });
    }
}
