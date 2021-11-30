package org.jrpq.rlci.core.kbs;

import org.jrpq.rlci.core.graphs.EdgeLabeledGraph;

import java.util.Iterator;

public abstract class BackwardKernelBfs<V, E> extends KernelBfs<V, E> {

    protected BackwardKernelBfs(EdgeLabeledGraph<V, E> edgeLabeledGraph) {
        super(edgeLabeledGraph, -1);
    }

    @Override
    protected Iterator<E> nextEdges(V vertex) {
        return edgeLabeledGraph.inEdgesIterator(vertex);
    }

    @Override
    protected V nextVertex(E edge) {
        return edgeLabeledGraph.getSourceOf(edge);
    }

    @Override
    protected int firstState(int lenOfKernel) {
        return lenOfKernel - 1;
    }

    @Override
    protected int lastState(int lenOfKernel) {
        return 0;
    }
}
