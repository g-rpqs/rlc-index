package org.jrpq.rlci.core.kbs;

import org.jrpq.rlci.core.graphs.EdgeLabeledGraph;

import java.util.Iterator;

public abstract class ForwardKernelSearch<V, E> extends KernelSearch<V, E> {

    protected ForwardKernelSearch(int k, EdgeLabeledGraph<V, E> edgeLabeledGraph, LabelSequenceCollection labelSequenceCollection) {
        super(k, edgeLabeledGraph, labelSequenceCollection);
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
    protected LabelSequence nextLabelSeq(LabelSequence currentLabelSeq, E edge) {
        return labelSequenceCollection.forwardExpand(currentLabelSeq, edgeLabeledGraph.getEncodedEdgeLabel(edge));
    }

    @Override
    protected int computeStateId(int lenOfKernel) {
        return lenOfKernel - 1;
    }
}
