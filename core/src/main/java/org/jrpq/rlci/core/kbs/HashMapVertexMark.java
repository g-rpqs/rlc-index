package org.jrpq.rlci.core.kbs;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

public class HashMapVertexMark implements VertexMark {
    private final IntOpenHashSet[] visited;

    public HashMapVertexMark(int stateSize) {
        this.visited = new IntOpenHashSet[stateSize];
        for (int i = 0; i < stateSize; i++)
            visited[i] = new IntOpenHashSet();
    }

    @Override
    public boolean markIfNotVisited(int uId, int stateId) {
        return !visited[stateId].add(uId);
    }

    @Override
    public boolean isVisited(int uId, int stateId) {
        return visited[stateId].contains(uId);
    }
}
