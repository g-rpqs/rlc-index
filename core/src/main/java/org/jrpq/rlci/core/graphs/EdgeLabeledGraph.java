package org.jrpq.rlci.core.graphs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public interface EdgeLabeledGraph<V, E> {
    int getEncodedEdgeLabel(E edge);

    int encodeEdgeLabel(String label);

    String decodeEdgeLabel(int encode);

    int getNumberOfVertices();

    int getNumberOfEdges();

    Iterator<E> outEdgesIterator(V v);

    Iterator<E> inEdgesIterator(V v);

    V getSourceOf(E e);

    V getTargetOf(E e);

    int getVertexId(V v);

    Iterator<V> getVertices();

    Iterator<E> getEdges();

    Iterator<V> sortVerticesBasedOnDegree();

    Set<String> getLabelSet();

    V getVertex(int vId);

    String getGraphName();

    default int[] encodeLabelConstraint(String[] constraints){
        if (constraints == null || constraints.length == 0)
            return null;
        int[] cons = new int[constraints.length];
        for (int i = 0, len = constraints.length; i < len; i++)
            cons[i] = encodeEdgeLabel(constraints[i]);
        return cons;
    }

    default String decodeLabelConstraint(int[] encodedCons){
        if (encodedCons == null || encodedCons.length == 0)
            return null;
        List<String> ret = new ArrayList<>();
        for(int l : encodedCons)
            ret.add(decodeEdgeLabel(l));
        return ret.toString();
    }
}
