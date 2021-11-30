package org.jrpq.rlci.core.kbs;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.Arrays;

public class LabelSequenceHashMap extends LabelSequenceCollection {

    private final ObjectOpenHashSet<LabelSequence> readOnlyLabelSequences;

    private final LabelSequence[] writeOnlyLabelSequences; // the length of i-th LabelSequence in this array is i+1

    private final Object2ObjectOpenHashMap<LabelSequence, MinimumRepeat> labelSequence2MinimumRepeat;

    private final LabelSequence epsilon;

    private final Object2ObjectOpenHashMap<LabelSequence, IntOpenHashSet> markings;

    protected LabelSequenceHashMap(int k) {
        readOnlyLabelSequences = new ObjectOpenHashSet<>();
        writeOnlyLabelSequences = new LabelSequence[2 * k];
        labelSequence2MinimumRepeat = new Object2ObjectOpenHashMap<>();
        epsilon = new LabelSequence(new int[0], 1, true);
        markings = new Object2ObjectOpenHashMap<>();
    }

    @Override
    public LabelSequence getEpsilon() {
        return epsilon;
    }

    @Override
    public LabelSequence forwardExpand(LabelSequence currentLabelSequence, int newEdgeLabel) {
        return getReadOnlyLabelSequence(forward(getWriteOnlyLabelSequence(currentLabelSequence.length() + 1), currentLabelSequence, newEdgeLabel));
    }

    LabelSequence forward(LabelSequence expanded, LabelSequence current, int newLabel) {
        int[] curSeq = current.sequence;
        System.arraycopy(curSeq, 0, expanded.sequence, 0, curSeq.length);
        expanded.sequence[expanded.sequence.length - 1] = newLabel;
        expanded.incrementalCode = current.hashCode() * 31 + newLabel;
        return expanded;
    }

    @Override
    public LabelSequence backwardExpand(LabelSequence currentLabelSequence, int newEdgeLabel) {
        return getReadOnlyLabelSequence(backward(getWriteOnlyLabelSequence(currentLabelSequence.length() + 1), currentLabelSequence, newEdgeLabel));
    }

    LabelSequence backward(LabelSequence expanded, LabelSequence current, int newLabel) {
        int[] curSeq = current.sequence;
        System.arraycopy(curSeq, 0, expanded.sequence, 1, curSeq.length);
        expanded.sequence[0] = newLabel;
        int temp = 1;
        int i = expanded.sequence.length - 1;
        while (i-- > 0)
            temp *= 31;
        expanded.incrementalCode = temp * 31 + temp * (newLabel - 1) + current.hashCode();
        return expanded;
    }

    private LabelSequence getWriteOnlyLabelSequence(int len) {
        LabelSequence writeOnly;
        if ((writeOnly = writeOnlyLabelSequences[len - 1]) == null) {
            int[] ints = new int[len];
            writeOnly = new LabelSequence(ints, Arrays.hashCode(ints), true);
            writeOnlyLabelSequences[len - 1] = writeOnly;
        }
        return writeOnly;
    }

    // The hashCode of writeOnly should be correct in case of incremental computation
    private LabelSequence getReadOnlyLabelSequence(LabelSequence writeOnly) { // return a read-only label sequence
        LabelSequence ret = readOnlyLabelSequences.get(writeOnly);
        if (ret != null)
            return ret;
        ret = writeOnly.copy(); // create a copy
        readOnlyLabelSequences.add(ret);
        return ret;
    }

    @Override
    public MinimumRepeat getMr(LabelSequence labelSequence) {
        MinimumRepeat mr;
        if ((mr = labelSequence2MinimumRepeat.get(labelSequence)) == null) {
            mr = MinimumRepeat.computeMinimumRepeat(labelSequence.sequence);
            labelSequence2MinimumRepeat.put(labelSequence, mr);
        }
        return mr;
    }

    @Override
    public boolean isVisited(int vId, LabelSequence labelSequence) {
        IntOpenHashSet intOpenHashSet;
        if ((intOpenHashSet = markings.get(labelSequence)) == null) {
            intOpenHashSet = new IntOpenHashSet();
            markings.put(labelSequence, intOpenHashSet);
        }
        return (!intOpenHashSet.add(vId));
    }

    @Override
    public void clearMarks() {
        for (IntOpenHashSet intOpenHashSet : markings.values())
            intOpenHashSet.clear();
    }
}
