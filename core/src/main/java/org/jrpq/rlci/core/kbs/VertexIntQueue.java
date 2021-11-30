package org.jrpq.rlci.core.kbs;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectIntMutablePair;
import it.unimi.dsi.fastutil.objects.ObjectIntPair;

import java.util.ArrayDeque;
import java.util.Queue;

public class VertexIntQueue<V> {
    private final ObjectIntPair<V> pair = ObjectIntMutablePair.of(null, 0);
    private final Queue<V> vertexQueue;
    private final IntArrayFIFOQueue stateQueue;

    public VertexIntQueue() {
        vertexQueue = new ArrayDeque<>();
        stateQueue = new IntArrayFIFOQueue();
    }

    public void enqueue(V vertex, int stateId) {
        vertexQueue.add(vertex);
        stateQueue.enqueue(stateId);
    }

    public ObjectIntPair<V> dequeue() {
        pair.left(vertexQueue.poll());
        return pair.right(stateQueue.dequeueInt());
    }

    public boolean isEmpty() {
        return vertexQueue.isEmpty();
    }

    public int size() {
        return vertexQueue.size();
    }

}
