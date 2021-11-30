package org.jrpq.rlci.benchmark.util.generators;

import org.jrpq.rlci.core.graphs.EdgeLabeledGraph;
import org.jrpq.rlci.core.kbs.HashMapVertexMark;
import org.jrpq.rlci.core.kbs.VertexMark;
import it.unimi.dsi.fastutil.Pair;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

public class QueryGenerator {
    public static <V, E> Pair<Boolean, Integer> generateQueriesUsingBfs(EdgeLabeledGraph<V, E> edgeLabeledGraph, V source, V target, String[] constraints) {
        int[] cons = edgeLabeledGraph.encodeLabelConstraint(constraints);
        Queue<Node<V>> q = new ArrayDeque<>();
        q.add(new Node<>(source, 0, 0));
        VertexMark vertexMark = new HashMapVertexMark(cons.length);
        int currentDistance = 0; // the returned distance for false-queries
        while (!q.isEmpty()) {
            Node<V> node = q.poll();
            currentDistance = Math.max(currentDistance, node.d);
            V vertex = node.vertex;
            int stateId = node.stateId;
            if (stateId == cons.length) {
                if (vertex.equals(target))
                    return Pair.of(true, node.d);
                stateId = 0;
            }
            Iterator<E> outEdges = edgeLabeledGraph.outEdgesIterator(vertex);
            while (outEdges.hasNext()) {
                E edge = outEdges.next();
                V u = edgeLabeledGraph.getTargetOf(edge);
                if (edgeLabeledGraph.getEncodedEdgeLabel(edge) != cons[stateId] || vertexMark.markIfNotVisited(edgeLabeledGraph.getVertexId(u), stateId))
                    continue;
                q.add(new Node<>(u, stateId + 1, node.d + 1));
            }
        }
        return Pair.of(false, currentDistance);
    }

    public static <V, E> Pair<Boolean, Integer> generateQueriesUsingBiBfS(EdgeLabeledGraph<V, E> edgeLabeledGraph, V source, V target, String[] constraints) {
        int[] cons = edgeLabeledGraph.encodeLabelConstraint(constraints);
        VertexMark forwardVisited = new HashMapVertexMark(cons.length), backwardVisited = new HashMapVertexMark(cons.length);
        Queue<Node<V>> forwardQueue = new ArrayDeque<>(), backwardQueue = new ArrayDeque<>();
        forwardQueue.add(new Node<>(source, 0, 0));
        backwardQueue.add(new Node<>(target, cons.length - 1, 0));
        int fd = 0, bd = 0; // the returned distance
        while (!forwardQueue.isEmpty() && !backwardQueue.isEmpty()) {
            // forward start
            Node<V> fNode = forwardQueue.poll();
            fd = Math.max(fd, fNode.d);
            int forwardStateId = fNode.stateId;
            if (forwardStateId == cons.length)
                forwardStateId = 0;
            // check whether forward-vertex is visited by the backward search with the forward-state id
            if (backwardVisited.isVisited(edgeLabeledGraph.getVertexId(fNode.vertex), forwardStateId))
                return Pair.of(true, fd);
            // forward start
            Iterator<E> outEdges = edgeLabeledGraph.outEdgesIterator(fNode.vertex);
            while (outEdges.hasNext()) {
                E edge = outEdges.next();
                V u = edgeLabeledGraph.getTargetOf(edge);
                if (edgeLabeledGraph.getEncodedEdgeLabel(edge) != cons[forwardStateId] || forwardVisited.markIfNotVisited(edgeLabeledGraph.getVertexId(u), forwardStateId))
                    continue;
                forwardQueue.add(new Node<>(u, forwardStateId + 1, fNode.d + 1));
            }
            // forward end
            // ******************
            // backward start
            Node<V> bNode = backwardQueue.poll();
            bd = Math.max(bd, bNode.d);
            int backwardStateId = bNode.stateId;
            if (backwardStateId == -1)
                backwardStateId = cons.length - 1;
            // check whether backward-vertex is visited by the forward search with the backward-state id
            if (forwardVisited.isVisited(edgeLabeledGraph.getVertexId(bNode.vertex), backwardStateId))
                return Pair.of(true, bd);
            Iterator<E> inEdges = edgeLabeledGraph.inEdgesIterator(bNode.vertex);
            while (inEdges.hasNext()) {
                E edge = inEdges.next();
                V u = edgeLabeledGraph.getSourceOf(edge);
                if (edgeLabeledGraph.getEncodedEdgeLabel(edge) != cons[backwardStateId] || backwardVisited.markIfNotVisited(edgeLabeledGraph.getVertexId(u), backwardStateId))
                    continue;
                backwardQueue.add(new Node<>(u, backwardStateId - 1, bNode.d + 1));
            }
            // backward end
        }
        return Pair.of(false, Math.min(fd, bd));
    }

    private static class Node<V> {
        V vertex;
        int stateId;
        int d;

        public Node(V vertex, int stateId, int d) {
            this.vertex = vertex;
            this.stateId = stateId;
            this.d = d;
        }
    }
}
