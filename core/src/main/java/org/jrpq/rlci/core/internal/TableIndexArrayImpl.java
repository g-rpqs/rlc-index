package org.jrpq.rlci.core.internal;

import org.jrpq.rlci.core.kbs.MinimumRepeat;
import it.unimi.dsi.fastutil.ints.IntObjectMutablePair;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jgrapht.alg.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class TableIndexArrayImpl implements TableIndex {

    private static final long serialVersionUID = -4562197009400066266L;
    private final IndexRow[] lIn;
    private final IndexRow[] lOut;
    private final Object2IntOpenHashMap<MinimumRepeat> encoder;
    private final ObjectArrayList<MinimumRepeat> decoder;

    public TableIndexArrayImpl(int numOfVertices) {
        lIn = new IndexRow[numOfVertices];
        lOut = new IndexRow[numOfVertices];
        encoder = new Object2IntOpenHashMap<>();
        decoder = new ObjectArrayList<>();
    }

    @Override
    public void addOutEntry(int vOut, int s, MinimumRepeat mr, boolean isT) {
        add(lOut, vOut, s, mr, isT);
    }

    @Override
    public void addInEntry(int vIn, int s, MinimumRepeat mr, boolean isT) {
        add(lIn, vIn, s, mr, isT);
    }

    private int encodeMr(MinimumRepeat mr) {
        int value = encoder.getOrDefault(mr, -1);
        if (value == -1) {
            int encode = encoder.size();
            encoder.put(mr, encode);
            decoder.add(mr);
            return encode;
        } else
            return value;
    }

    private MinimumRepeat decodeMr(int encodedMr) {
        return decoder.get(encodedMr);
    }

    // Parameter isT is not used anymore in the current version of the RLC index.
    private void add(IndexRow[] inOrOut, int v, int u, MinimumRepeat mr, boolean isT) {
        IndexRow indexRow = inOrOut[v];
        if (indexRow == null) {
            indexRow = new BooleansIndexRow();
            indexRow.add(u, encodeMr(mr), isT);
            inOrOut[v] = indexRow;
        } else {
            int i = indexRow.getIndexFromEnd(u, encodeMr(mr));
            if (i == -1) { // not contained
                indexRow.add(u, encodeMr(mr), isT);
                if (indexRow.getSize() == IndexRow.THRESHOLD_TO_TRANSFORM)
                    inOrOut[v] = indexRow.transform();
            } else if (isT && !indexRow.getIsT(i)) // update isT from false to true
                indexRow.setIsT(i);
        }
    }

    private boolean checkCase2(IndexRow indexRow, int uid, int encodedCons) {
        IndexRow.IntIntPair range = indexRow.getTheRangeOf(uid);
        if (range == null)
            return false; // uid is not contained in the indexRow
        else
            return indexRow.contains(range.first, range.second, encodedCons);
    }

//    @Override
//    @Deprecated
//    public boolean queryWithConcatenation(int sid, int tid, MinimumRepeat constraints) {
//        int encodedCons = encoder.getOrDefault(constraints, -1); // if encodedCons = -1, then the constraint is not contained in sOut or tIn
//        if (encodedCons == -1)
//            return false;
//        IndexRow sIndexRow = lOut[sid], tIndexRow = lIn[tid];
//        if (sIndexRow == null) {
//            if (tIndexRow == null)
//                return false;
//            else
//                return checkCase2(tIndexRow, sid, encodedCons);
//        } else {
//            if (tIndexRow == null)
//                return checkCase2(sIndexRow, tid, encodedCons);
//            else if (checkCase2(sIndexRow, tid, encodedCons) || checkCase2(tIndexRow, sid, encodedCons))
//                return true;
//        }
//        int[] sOut = sIndexRow.getUIds(), tIn = tIndexRow.getUIds();
//        int sFrom = 0, tFrom = 0, sLen = sIndexRow.getSize(), tLen = tIndexRow.getSize();
//        while (sFrom < sLen && tFrom < tLen) {
//            if (sOut[sFrom] == tIn[tFrom]) {
//                int sV = sOut[sFrom], tV = tIn[tFrom], sTo = sFrom, tTo = tFrom;
//                while (sTo < sLen && sOut[sTo] == sV)
//                    sTo++;
//                while (tTo < tLen && tIn[tTo] == tV)
//                    tTo++;
//                int sRange = sTo - sFrom, tRange = tTo - tFrom; // fromIndex (inclusive), toIndex (exclusive)
//                if (sIndexRow.contains(sFrom, sTo, encodedCons) && tIndexRow.contains(tFrom, tTo, encodedCons)) // check case 1.2
//                    return true;
//                int[] prefixCandidates = new int[sRange], suffixCandidates = new int[tRange];
//                int prefixSize = 0, suffixSize = 0;
//                for (int i = sFrom; i < sTo; i++) {
//                    if (sIndexRow.getIsT(i)) { // true-MR
//                        int prefix = MinimumRepeat.prefixCheck(decodeMr(sIndexRow.getMr(i)), constraints);
//                        if (prefix != -1) // invalid candidate for prefix
//                            prefixCandidates[prefixSize++] = prefix;
//                    }
//                }
//                if (prefixSize == 0) {
//                    sFrom = sTo; // start from toIndex
//                    tFrom = tTo; // start from toIndex
//                    continue;
//                }
//                for (int i = tFrom; i < tTo; i++) {
//                    if (tIndexRow.getIsT(i)) {// true-MR
//                        int suffix = MinimumRepeat.suffixCheck(decodeMr(tIndexRow.getMr(i)), constraints);
//                        if (suffix != -1) // invalid candidate for suffix
//                            suffixCandidates[suffixSize++] = suffix;
//                    }
//                }
//                if (suffixSize == 0) {
//                    sFrom = sTo;
//                    tFrom = tTo;
//                    continue;
//                }
//                int lengthOfConstraint = constraints.length();
//                for (int i = 0; i < prefixSize; i++) {
//                    int validSuffix = (prefixCandidates[i] == lengthOfConstraint - 1) ? 0 : prefixCandidates[i] + 1;
//                    for (int j = 0; j < suffixSize; j++)
//                        if (suffixCandidates[j] == validSuffix)
//                            return true;
//                }
//                sFrom = sTo;
//                tFrom = tTo;
//            } else if (sOut[sFrom] > tIn[tFrom])
//                tFrom++;
//            else
//                sFrom++;
//        }
//        return false;
//    }

    @Override
    public boolean queryWithoutConcatenation(int sid, int tid, MinimumRepeat constraints) {
        if (constraints == null)
            return false;
        int encodedCons = encoder.getOrDefault(constraints, -1); // if encodedCons = -1, then the constraint is not contained in sOut or tIn
        if (encodedCons == -1)
            return false;
        IndexRow sIndexRow = lOut[sid], tIndexRow = lIn[tid];
        if (sIndexRow == null) {
            if (tIndexRow == null)
                return false;
            else
                return checkCase2(tIndexRow, sid, encodedCons);
        } else {
            if (tIndexRow == null)
                return checkCase2(sIndexRow, tid, encodedCons);
            else if (checkCase2(sIndexRow, tid, encodedCons) || checkCase2(tIndexRow, sid, encodedCons))
                return true;
        }
        int[] sOut = sIndexRow.getUIds(), tIn = tIndexRow.getUIds();
        int sFrom = 0, tFrom = 0, sLen = sIndexRow.getSize(), tLen = tIndexRow.getSize();
        while (sFrom < sLen && tFrom < tLen) {
            if (sOut[sFrom] == tIn[tFrom]) {
                int sV = sOut[sFrom], tV = tIn[tFrom], sTo = sFrom, tTo = tFrom;
                while (sTo < sLen && sOut[sTo] == sV)
                    sTo++;
                while (tTo < tLen && tIn[tTo] == tV)
                    tTo++;
                if (sIndexRow.contains(sFrom, sTo, encodedCons) && tIndexRow.contains(tFrom, tTo, encodedCons)) // check case 1.2
                    return true;
                sFrom = sTo;
                tFrom = tTo;
            } else if (sOut[sFrom] > tIn[tFrom])
                tFrom++;
            else
                sFrom++;
        }
        return false;
    }

    //False-negative results
    //Only for internal usage
    public boolean queryAPlusBPlus(int sid, int tid, MinimumRepeat a, MinimumRepeat b) { //a+b+
        if (a == null || b == null)
            return false;
        int encodedConsA = encoder.getOrDefault(a, -1), encodedConsB = encoder.getOrDefault(b, -1); // if encodedCons = -1, then the constraint is not contained in sOut or tIn
        if (encodedConsA == -1 || encodedConsB == -1)
            return false;
        IndexRow sIndexRow = lOut[sid], tIndexRow = lIn[tid];
        if (sIndexRow == null || tIndexRow == null)
            return false;
        int[] sOut = sIndexRow.getUIds(), tIn = tIndexRow.getUIds();
        int sFrom = 0, tFrom = 0, sLen = sIndexRow.getSize(), tLen = tIndexRow.getSize();
        while (sFrom < sLen && tFrom < tLen) {
            if (sOut[sFrom] == tIn[tFrom]) {
                int sV = sOut[sFrom], tV = tIn[tFrom], sTo = sFrom, tTo = tFrom;
                while (sTo < sLen && sOut[sTo] == sV)
                    sTo++;
                while (tTo < tLen && tIn[tTo] == tV)
                    tTo++;
                if (sIndexRow.contains(sFrom, sTo, encodedConsA) && tIndexRow.contains(tFrom, tTo, encodedConsB)) // check case 1.2
                    return true; // a+b+
                sFrom = sTo;
                tFrom = tTo;
            } else if (sOut[sFrom] > tIn[tFrom])
                tFrom++;
            else
                sFrom++;
        }
        return false;
    }


    // BackwardBFS from t, such that if an index row contains index entries for vertex t, then such index entries are at the end of the index row
    // This can avoid applying binary search to check whether there exists (t, constraints) in Lout(s)
    @Override
    public boolean backwardQuery(int sId, int tId, MinimumRepeat constraints) {
        IndexRow sIndexRow = lOut[sId], tIndexRow = lIn[tId];
        int encodedConstraint = encoder.getOrDefault(constraints, -1); // if encodedCons = -1, then the constraint is not contained in sOut or tIn
        if (encodedConstraint == -1)
            return false;
        // Because of PR2, we have sid >= tid.
        // The optimized version to check case2, whether there exists (tId, cons) in Lout(s) without using binary search, i.e., from the end of Lout(s)
        // At this step, we must have sId >= tId because of PR2.
        // In case of sId = tId, the backward BFS from t is preformed first, such that there does not exist (sId, cons) in Lin(t)
        // In case of sId > tId, vertex s has not been processed, such that there does not exist (sId, cons) in Lin(t)
        // Therefore, we only need to check whether there exists (tId, cons) in Lout(s).
        // Given this, since the current backward BFS is performed from vertex t, such that the index entry (tId, cons) is at the end of Lout(s).
        if (sIndexRow != null && sIndexRow.getIndexFromEnd(tId, encodedConstraint) != -1)
            return true;
        if (sIndexRow == null || tIndexRow == null)
            return false;
        else return checkCase1WithoutConcatenation(sIndexRow, tIndexRow, encodedConstraint);
    }

    // ForwardBFS from s, such that if an index row contains index entries for vertex s, then such index entries are at the end of the index row
    // This can avoid applying binary search to check whether there exists (s, constraints) in Lin(t)
    @Override
    public boolean forwardQuery(int sId, int tId, MinimumRepeat constraints) {
        IndexRow sIndexRow = lOut[sId], tIndexRow = lIn[tId];
        int encodedConstraint = encoder.getOrDefault(constraints, -1); // if encodedCons = -1, then the constraint is not contained in sOut or tIn
        if (encodedConstraint == -1)
            return false;
        // Because of PR2, we have sid <= tid.
        // The optimized version to check case2, whether there exists (tId, cons) in Lout(s) without using binary search, i.e., from the end of Lout(s)
        // At this step, we must have sId <= tId because of PR2.
        // In case of sId = tId, the backward BFS from t is preformed first, such that there may exist (tId, cons) in Lout(s). Given this, if there does not exist (tId, cons) in Lout(s), there must not exist (sId, cons) in Lin(t) because of sId = tId.
        // In case of sId < tId, vertex t has not been processed, such that there does not exist (tId, cons) in Lout(s), Therefore, we only need to check whether there exists (sId, cons) in Lin(t).
        // Given this, since the current forward BFS is performed from vertex s, such that the index entry (sId, cons) is at the end of Lin(t).
        if (sId == tId) {
//            if (sIndexRow.getIndexFromEnd(tId, encodedConstraint) != -1 || tIndexRow.getIndexFromEnd(sId, encodedConstraint) != -1)
//                return true;
            if (sIndexRow.getIndexFromEnd(tId, encodedConstraint) != -1)
                return true;
        } else {
            if (tIndexRow != null && tIndexRow.getIndexFromEnd(sId, encodedConstraint) != -1)
                return true;
        }
        if (sIndexRow == null || tIndexRow == null)
            return false;
        else return checkCase1WithoutConcatenation(sIndexRow, tIndexRow, encodedConstraint);
    }

    @Override
    public List<Pair<List<IntObjectPair<MinimumRepeat>>, List<IntObjectPair<MinimumRepeat>>>> getIndexView() {
        int len = lIn.length;
        List<Pair<List<IntObjectPair<MinimumRepeat>>, List<IntObjectPair<MinimumRepeat>>>> ret = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
            final List<IntObjectPair<MinimumRepeat>> in = new ArrayList<>(), out = new ArrayList<>();
            if (lIn[i] != null)
                lIn[i].getIndexRowView().forEach(intIntPair -> in.add(IntObjectMutablePair.of(intIntPair.first, decodeMr(intIntPair.second))));
            else
                in.add(null);
            if (lOut[i] != null)
                lOut[i].getIndexRowView().forEach(intIntPair -> out.add(IntObjectMutablePair.of(intIntPair.first, decodeMr(intIntPair.second))));
            else
                out.add(null);
            ret.add(Pair.of(in, out));
        }
        return ret;
    }

    private boolean checkCase1WithoutConcatenation(IndexRow sIndexRow, IndexRow tIndexRow, int encodedConstraint) {
        int[] sOut = sIndexRow.getUIds(), tIn = tIndexRow.getUIds();
        int sFrom = 0, tFrom = 0, sSiz = sIndexRow.getSize(), tSize = tIndexRow.getSize();
        while (sFrom < sSiz && tFrom < tSize) {
            if (sOut[sFrom] == tIn[tFrom]) {
                int sV = sOut[sFrom], tV = tIn[tFrom], sTo = sFrom, tTo = tFrom;
                while (sTo < sSiz && sOut[sTo] == sV)
                    sTo++;
                while (tTo < tSize && tIn[tTo] == tV)
                    tTo++;
                if (sIndexRow.contains(sFrom, sTo, encodedConstraint) && tIndexRow.contains(tFrom, tTo, encodedConstraint)) // check case 1
                    return true;
                sFrom = sTo; // sOut[sTo] != sV
                tFrom = tTo; // tIn[sTo] != tV
            } else if (sOut[sFrom] > tIn[tFrom])
                tFrom++;
            else
                sFrom++;
        }
        return false;
    }
}
