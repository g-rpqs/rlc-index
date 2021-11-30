package org.jrpq.rlci.core.kbs;

public interface VertexMark {

    // returns true if u has been visited by the state kernel[stateId], otherwise return false and mark u visited by with the state
    boolean markIfNotVisited(int uId, int stateId);

    // check if u has been visited by the state
    boolean isVisited(int uId, int stateId);
}
