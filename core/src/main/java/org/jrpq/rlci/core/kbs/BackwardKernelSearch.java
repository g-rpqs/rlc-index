package org.jrpq.rlci.core.kbs;

import org.jrpq.rlci.core.graphs.EdgeLabeledGraph;

import java.util.Iterator;

public abstract class BackwardKernelSearch<V, E> extends KernelSearch<V, E> {

    protected BackwardKernelSearch(int k, EdgeLabeledGraph<V, E> edgeLabeledGraph, LabelSequenceCollection labelSequenceCollection) {
        super(k, edgeLabeledGraph, labelSequenceCollection);
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
    protected LabelSequence nextLabelSeq(LabelSequence currentLabelSeq, E edge) {
        return labelSequenceCollection.backwardExpand(currentLabelSeq, edgeLabeledGraph.getEncodedEdgeLabel(edge));
    }

    @Override
    protected int computeStateId(int lenOfKernel) {
        return 0;
    }
}
