package org.jrpq.rlci.core.kbs;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectObjectMutablePair;

import java.util.ArrayDeque;
import java.util.Queue;

public class VertexLabelSeqQueue<V> {
    private final Pair<V, LabelSequence> pair = ObjectObjectMutablePair.of(null, null);

    private final Queue<V> vertexQueue;

    private final Queue<LabelSequence> labelSequenceQueue;

    public VertexLabelSeqQueue() {
        vertexQueue = new ArrayDeque<>();
        labelSequenceQueue = new ArrayDeque<>();
    }

    public void enqueue(V vertex, LabelSequence labelSequence) {
        vertexQueue.add(vertex);
        labelSequenceQueue.add(labelSequence);
    }

    public Pair<V, LabelSequence> dequeue() {
        return pair.left(vertexQueue.poll()).right((labelSequenceQueue.poll()));
    }

    public boolean isEmpty() {
        return vertexQueue.isEmpty();
    }
}
