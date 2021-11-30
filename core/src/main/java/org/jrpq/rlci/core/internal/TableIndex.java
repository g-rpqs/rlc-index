package org.jrpq.rlci.core.internal;

import org.jrpq.rlci.core.kbs.MinimumRepeat;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import org.jgrapht.alg.util.Pair;

import java.io.Serializable;
import java.util.List;

public interface TableIndex extends Serializable {
    void addOutEntry(int vOut, int s, MinimumRepeat mr, boolean isT);

    void addInEntry(int vIn, int s, MinimumRepeat mr, boolean isT);

//    @Deprecated
//    boolean queryWithConcatenation(int sid, int tid, MinimumRepeat constraints);

    boolean queryWithoutConcatenation(int sid, int tid, MinimumRepeat constraints);

    boolean queryAPlusBPlus(int sid, int tid, MinimumRepeat a, MinimumRepeat b);

    boolean backwardQuery(int sid, int tid, MinimumRepeat constraints);

    boolean forwardQuery(int sid, int tid, MinimumRepeat constraints);

    List<Pair<List<IntObjectPair<MinimumRepeat>>,List<IntObjectPair<MinimumRepeat>>>> getIndexView();
}
