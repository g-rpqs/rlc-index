package org.jrpq.rlci.core;

import org.jrpq.rlci.core.graphs.EdgeLabeledGraph;
import org.jrpq.rlci.core.kbs.MinimumRepeat;
import org.jrpq.rlci.core.kbs.VertexIntQueue;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectIntPair;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ExtendedQuerySupport {

    public static <V, E> boolean queryQ3(RlcIndex<V, E> index, V source, V target, String a, String b) { // query: ab+
        EdgeLabeledGraph<V, E> edgeLabeledGraph = index.getEdgeLabeledGraph();
        int encodedLabelA = edgeLabeledGraph.encodeEdgeLabel(a);
        MinimumRepeat mr = MinimumRepeat.computeMinimumRepeat(edgeLabeledGraph.encodeLabelConstraint(new String[]{b}));
        int tId = index.getAccessIdOf(target);
        Iterator<E> eIterator = edgeLabeledGraph.outEdgesIterator(source);
        while (eIterator.hasNext()) {
            E edge = eIterator.next();
            if (edgeLabeledGraph.getEncodedEdgeLabel(edge) == encodedLabelA && index.queryWithAccessId(index.getAccessIdOf(edgeLabeledGraph.getTargetOf(edge)), tId, mr))
                return true;
        }
        return false;
    }

    public static <V, E> boolean queryQ4(RlcIndex<V, E> index, V source, V target, String a, String b, String c) { // query: ab+c
        EdgeLabeledGraph<V, E> edgeLabeledGraph = index.getEdgeLabeledGraph();
        MinimumRepeat mr = MinimumRepeat.computeMinimumRepeat(edgeLabeledGraph.encodeLabelConstraint(new String[]{b}));
        int encodedLabelA = edgeLabeledGraph.encodeEdgeLabel(a),
                encodedLabelC = edgeLabeledGraph.encodeEdgeLabel(c);
        IntList sourceList = getNeighboursWithEdgeLabel(edgeLabeledGraph, index, source, encodedLabelA, true),
                targetList = getNeighboursWithEdgeLabel(edgeLabeledGraph, index, target, encodedLabelC, false);
        if (sourceList.size() > 0 && targetList.size() > 0) {
            for (int i = 0; i < sourceList.size(); i++) {
                int tempSource = sourceList.getInt(i);
                for (int j = 0; j < targetList.size(); j++) {
                    if (index.queryWithAccessId(tempSource, targetList.getInt(j), mr))
                        return true;
                }
            }
        }
        return false;
    }

    // return list of accessId of vertices
    private static <V, E> IntList getNeighboursWithEdgeLabel(EdgeLabeledGraph<V, E> edgeLabeledGraph, RlcIndex<V, E> index, V vertex, int encodedLabel, boolean isOutgoing) {
        IntList intList = new IntArrayList();
        if (isOutgoing) {
            Iterator<E> outEdgesIterator = edgeLabeledGraph.outEdgesIterator(vertex);
            while (outEdgesIterator.hasNext()) {
                E edge = outEdgesIterator.next();
                if (edgeLabeledGraph.getEncodedEdgeLabel(edge) == encodedLabel)
                    intList.add(index.getAccessIdOf(edgeLabeledGraph.getTargetOf(edge)));
            }
        } else {
            Iterator<E> inEdgesIterator = edgeLabeledGraph.inEdgesIterator(vertex);
            while (inEdgesIterator.hasNext()) {
                E edge = inEdgesIterator.next();
                if (edgeLabeledGraph.getEncodedEdgeLabel(edge) == encodedLabel)
                    intList.add(index.getAccessIdOf(edgeLabeledGraph.getSourceOf(edge)));
            }
        }
        return intList;
    }

    public static <V, E> boolean queryQ5(RlcIndex<V, E> index, V source, V target, String labelA, String labelB) { // query: a+b+
        EdgeLabeledGraph<V, E> edgeLabeledGraph = index.getEdgeLabeledGraph();
        MinimumRepeat mrA = MinimumRepeat.computeMinimumRepeat(edgeLabeledGraph.encodeLabelConstraint(new String[]{labelA}));
        MinimumRepeat mrB = MinimumRepeat.computeMinimumRepeat(edgeLabeledGraph.encodeLabelConstraint(new String[]{labelB}));
        int encodedLabelA = edgeLabeledGraph.encodeEdgeLabel(labelA), encodedLabelB = edgeLabeledGraph.encodeEdgeLabel(labelB);
        int sId = index.getAccessIdOf(source), tId = index.getAccessIdOf(target); // access ID
        if (index.getIndex().queryAPlusBPlus(sId, tId, mrA, mrB)) //Constraint: a+b+. The result may contain false negatives, such that ongoing traversal is necessary.
            return true;
        IntOpenHashSet
                forwardA = new IntOpenHashSet(),
                forwardB = new IntOpenHashSet(),
                backwardA = new IntOpenHashSet(),
                backwardB = new IntOpenHashSet();
        VertexIntQueue<V>
                forwardQueue = new VertexIntQueue<>(),
                backwardQueue = new VertexIntQueue<>();
        Iterator<E>
                outEdgesOfSource = edgeLabeledGraph.outEdgesIterator(source),
                inEdgesOfTarget = edgeLabeledGraph.inEdgesIterator(target);
        while (outEdgesOfSource.hasNext()) {// From source, the first outgoing edge must be A
            E e = outEdgesOfSource.next();
            if (edgeLabeledGraph.getEncodedEdgeLabel(e) == encodedLabelA) {
                V v = edgeLabeledGraph.getTargetOf(e);
                int tempSId = index.getAccessIdOf(v);
                if (index.getIndex().queryAPlusBPlus(tempSId, tId, mrA, mrB) || index.queryWithAccessId(tempSId, tId, mrB)) // a+b+, or b+
                    return true;
                forwardQueue.enqueue(v, encodedLabelA);
                forwardA.add(tempSId); // mark v as visited by label A
            }
        }
        while (inEdgesOfTarget.hasNext()) {// From target, the first incoming edge must be B
            E e = inEdgesOfTarget.next();
            if (edgeLabeledGraph.getEncodedEdgeLabel(e) == encodedLabelB) {
                V v = edgeLabeledGraph.getSourceOf(e);
                int tempTId = index.getAccessIdOf(v);
                if (index.getIndex().queryAPlusBPlus(sId, tempTId, mrA, mrB) || index.queryWithAccessId(sId, tempTId, mrA))
                    return true;
                backwardQueue.enqueue(v, encodedLabelB); // state until v
                backwardB.add(tempTId);
            }
        }

        return biBfsWithIndex(
                index,
                forwardQueue,
                backwardQueue,
                encodedLabelA,
                encodedLabelB,
                forwardA,
                forwardB,
                backwardA,
                backwardB,
                mrA,
                mrB);
    }

    public static <V, E> boolean queryQ6(RlcIndex<V, E> index, V source, V target, String labelA, String labelB, String labelC) { //ab+c+
        EdgeLabeledGraph<V, E> edgeLabeledGraph = index.getEdgeLabeledGraph();
        MinimumRepeat mrB = MinimumRepeat.computeMinimumRepeat(edgeLabeledGraph.encodeLabelConstraint(new String[]{labelB}));
        MinimumRepeat mrC = MinimumRepeat.computeMinimumRepeat(edgeLabeledGraph.encodeLabelConstraint(new String[]{labelC}));
        int encodeEdgeLabelA = edgeLabeledGraph.encodeEdgeLabel(labelA),
                encodedLabelB = edgeLabeledGraph.encodeEdgeLabel(labelB),
                encodedLabelC = edgeLabeledGraph.encodeEdgeLabel(labelC);
        IntList vList = getNeighboursWithEdgeLabel(edgeLabeledGraph, index, source, encodeEdgeLabelA, true);
        int tId = index.getAccessIdOf(target);
        if (vList.size() == 0)
            return false;
        for (int v : vList)
            if (index.getIndex().queryAPlusBPlus(v, tId, mrB, mrC))
                return true;
        IntOpenHashSet
                forwardB = new IntOpenHashSet(),
                forwardC = new IntOpenHashSet(),
                backwardB = new IntOpenHashSet(),
                backwardC = new IntOpenHashSet();
        VertexIntQueue<V>
                forwardQueue = new VertexIntQueue<>(),
                backwardQueue = new VertexIntQueue<>();
        IntList tempSourceList = new IntArrayList();
        for (int vId : vList) {
            V v = index.getVOfAccessId(vId);
            Iterator<E> outEdgesOfV = edgeLabeledGraph.outEdgesIterator(v);
            while (outEdgesOfV.hasNext()) {
                E e = outEdgesOfV.next();
                if (edgeLabeledGraph.getEncodedEdgeLabel(e) == encodedLabelB) {
                    V tempSource = edgeLabeledGraph.getTargetOf(e);
                    int tempSId = index.getAccessIdOf(tempSource);
                    if (index.getIndex().queryAPlusBPlus(tempSId, tId, mrB, mrC) || index.queryWithAccessId(tempSId, tId, mrC))
                        return true;
                    forwardQueue.enqueue(tempSource, encodedLabelB);
                    forwardB.add(tempSId);
                    tempSourceList.add(tempSId);
                }
            }
        }
        Iterator<E> inEdgesOfTarget = edgeLabeledGraph.inEdgesIterator(target);
        while (inEdgesOfTarget.hasNext()) {
            E e = inEdgesOfTarget.next();
            if (edgeLabeledGraph.getEncodedEdgeLabel(e) == encodedLabelC) {
                V tempTarget = edgeLabeledGraph.getSourceOf(e);
                int tempTId = index.getAccessIdOf(tempTarget);
                for (int tempSId : tempSourceList) {
                    if (index.getIndex().queryAPlusBPlus(tempSId, tempTId, mrB, mrC) || index.queryWithAccessId(tempSId, tempTId, mrB))
                        return true;
                }
                backwardQueue.enqueue(tempTarget, encodedLabelC);
                backwardC.add(tempTId);
            }
        }

        return biBfsWithIndex(
                index,
                forwardQueue,
                backwardQueue,
                encodedLabelB,
                encodedLabelC,
                forwardB,
                forwardC,
                backwardB,
                backwardC,
                mrB,
                mrC
        );
    }

    // A represents the first recursive label
    // B represents the second recursive label
    private static <V, E> boolean biBfsWithIndex(
            RlcIndex<V, E> index,
            VertexIntQueue<V> forwardQueue,
            VertexIntQueue<V> backwardQueue,
            int encodedFL,
            int encodedSL,
            IntOpenHashSet forwardFL,
            IntOpenHashSet forwardSL,
            IntOpenHashSet backwardFL,
            IntOpenHashSet backwardSL,
            MinimumRepeat mrFL,
            MinimumRepeat mrSL) {
        EdgeLabeledGraph<V, E> edgeLabeledGraph = index.getEdgeLabeledGraph();
        while (!forwardQueue.isEmpty() && !backwardQueue.isEmpty()) {
            List<ObjectIntPair<V>> fPairList = new ArrayList<>(), bPairList = new ArrayList<>();
            while (!forwardQueue.isEmpty())
                fPairList.add(forwardQueue.dequeue());
            while (!backwardQueue.isEmpty())
                bPairList.add(backwardQueue.dequeue());

            for (ObjectIntPair<V> fPair : fPairList) { // level-based processing
                int tempSId = index.getAccessIdOf(fPair.left());
                if (fPair.rightInt() == encodedFL) {// A
                    if (backwardFL.contains(tempSId) || backwardSL.contains(tempSId))
                        return true;
                } else { // B
                    if (backwardSL.contains(tempSId))
                        return true;
                }
            }

            for (ObjectIntPair<V> bPair : bPairList) { // level-based processing
                int tempTId = index.getAccessIdOf(bPair.left());
                if (bPair.rightInt() == encodedSL) { //B
                    if (forwardFL.contains(tempTId) || forwardSL.contains(tempTId))
                        return true;
                } else { //A
                    if (forwardFL.contains(tempTId))
                        return true;
                }
            }

            for (ObjectIntPair<V> fPair : fPairList) { // level-based processing, cross product
                int tempSId = index.getAccessIdOf(fPair.left());
                for (ObjectIntPair<V> bPair : bPairList) {
                    int tempTId = index.getAccessIdOf(bPair.left());
                    if (bPair.rightInt() == encodedSL) { //B
                        // query
                        if (fPair.rightInt() == encodedFL) {//A
                            if (index.getIndex().queryAPlusBPlus(tempSId, tempTId, mrFL, mrSL) || index.queryWithAccessId(tempSId, tempTId, mrFL) || index.queryWithAccessId(tempSId, tempTId, mrSL))
                                return true;
                        } else { //B
                            if (index.queryWithAccessId(tempSId, tempTId, mrSL))
                                return true;
                        }
                    } else { //A
                        // query
                        if (fPair.rightInt() == encodedFL) {
                            if (index.queryWithAccessId(tempSId, tempTId, mrFL))
                                return true;
                        }
                    }
                }
            }

            for (ObjectIntPair<V> fPair : fPairList) { // Continue traversing
                Iterator<E> outEdgesIterator = edgeLabeledGraph.outEdgesIterator(fPair.left());
                int currentLabel = fPair.rightInt();
                while (outEdgesIterator.hasNext()) {
                    E e = outEdgesIterator.next();
                    V nextV = edgeLabeledGraph.getTargetOf(e);
                    int nextVId = index.getAccessIdOf(nextV);
                    int nextEncodedEdgeLabel = edgeLabeledGraph.getEncodedEdgeLabel(e);
                    if (currentLabel == encodedFL) { // a
                        if (nextEncodedEdgeLabel == encodedFL && forwardFL.add(nextVId)) { // a -> a
                            forwardQueue.enqueue(nextV, encodedFL);
                        } else if (nextEncodedEdgeLabel == encodedSL && forwardSL.add(nextVId)) { // a -> b
                            forwardQueue.enqueue(nextV, encodedSL);
                        }
                    } else { // b
                        if (nextEncodedEdgeLabel == encodedSL && forwardSL.add(nextVId)) // b -> b
                            forwardQueue.enqueue(nextV, encodedSL);
                    }
                }
            }

            for (ObjectIntPair<V> bPair : bPairList) { // Continue traversing
                Iterator<E> inEdgesIterator = edgeLabeledGraph.inEdgesIterator(bPair.left());
                int currentLabel = bPair.rightInt();
                while (inEdgesIterator.hasNext()) {
                    E e = inEdgesIterator.next();
                    V nextV = edgeLabeledGraph.getSourceOf(e);
                    int nextVId = index.getAccessIdOf(nextV);
                    int nextEncodedEdgeLabel = edgeLabeledGraph.getEncodedEdgeLabel(e);
                    if (currentLabel == encodedSL) { // b
                        if (nextEncodedEdgeLabel == encodedSL && backwardSL.add(nextVId)) { //b -> b
                            backwardQueue.enqueue(nextV, encodedSL);
                        } else if (nextEncodedEdgeLabel == encodedFL && backwardFL.add(nextVId)) { // a -> b
                            backwardQueue.enqueue(nextV, encodedFL);
                        }
                    } else { // a
                        if (nextEncodedEdgeLabel == encodedFL && backwardFL.add(nextVId)) // a -> a
                            backwardQueue.enqueue(nextV, encodedFL);
                    }
                }
            }
        }
        return false;
    }
}
