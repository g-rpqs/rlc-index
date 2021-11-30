package org.jrpq.rlci.core.kbs;

import java.util.Arrays;

public class LabelSequence {
    final int[] sequence;
    final boolean useIc;
    int incrementalCode;

    public LabelSequence(int[] sequence, int incrementalCode) {
        this.sequence = sequence;
        this.incrementalCode = incrementalCode;
        this.useIc = false;
    }

    public LabelSequence(int[] sequence, int incrementalCode, boolean useIc) {
        this.sequence = sequence;
        this.incrementalCode = incrementalCode;
        this.useIc = useIc;
    }

    public LabelSequence copy() {
        int[] ints = new int[sequence.length];
        System.arraycopy(sequence, 0, ints, 0, ints.length);
        return new LabelSequence(ints, incrementalCode, useIc);
    }

    public int get(int index) {
        return sequence[index];
    }

    public int length() {
        return sequence.length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        LabelSequence that = (LabelSequence) o;
        if (sequence.length != that.length())
            return false;
        else {
            for (int i = 0, j = sequence.length; i < j; i++)
                if (sequence[i] != that.sequence[i])
                    return false;
            return true;
        }
    }

    @Override
    public int hashCode() {
        return useIc ? incrementalCode : Arrays.hashCode(sequence);
    }

    @Override
    public String toString() {
        return "LabelSequence{" +
                "sequence=" + Arrays.toString(sequence) +
                '}';
    }
}
