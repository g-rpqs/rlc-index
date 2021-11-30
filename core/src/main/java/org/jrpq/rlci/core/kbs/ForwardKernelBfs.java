package org.jrpq.rlci.core.kbs;

import org.jrpq.rlci.core.graphs.EdgeLabeledGraph;

import java.util.Iterator;

public abstract class ForwardKernelBfs<V, E> extends KernelBfs<V, E> {

    protected ForwardKernelBfs(EdgeLabeledGraph<V, E> edgeLabeledGraph) {
        super(edgeLabeledGraph, 1);
    }

    @Override
    protected Iterator<E> nextEdges(V vertex) {
        return edgeLabeledGraph.outEdgesIterator(vertex);
    }

    @Override
    protected V nextVertex(E edge) {
        return edgeLabeledGraph.getTargetOf(edge);
    }

    @Override
    protected int firstState(int lenOfKernel) {
        return 0;
    }

    @Override
    protected int lastState(int lenOfKernel) {
        return lenOfKernel - 1;
    }
}
