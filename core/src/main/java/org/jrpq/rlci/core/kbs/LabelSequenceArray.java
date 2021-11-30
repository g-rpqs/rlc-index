package org.jrpq.rlci.core.kbs;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

public class LabelSequenceArray extends LabelSequenceCollection {

    private static final LabelSequence EPSILON = new LabelSequence(new int[0], 0);
    private final LabelSequence[][] labelSequenceArray;
    private final MinimumRepeat[][] labelSequence2MinimumRepeat;
    private final IntOpenHashSet[][] markings;
    private final int numOfDigits, numOfBitsForEachDigit;

    private final int[] weights; // (1, ..., numOfLabels^{weights.length - 1})

    protected LabelSequenceArray(int numOfLabels, int maxLabelLength) {
        labelSequenceArray = new LabelSequence[maxLabelLength][];
        labelSequence2MinimumRepeat = new MinimumRepeat[maxLabelLength][];
        markings = new IntOpenHashSet[maxLabelLength][];
        weights = new int[maxLabelLength];
        double log2OfNumOfLabels = Math.log(numOfLabels) / Math.log(2);
        this.numOfBitsForEachDigit = Math.floor(log2OfNumOfLabels) == log2OfNumOfLabels ? (int) (log2OfNumOfLabels) : (int) (Math.floor(log2OfNumOfLabels) + 1);
        this.numOfDigits = 1 << numOfBitsForEachDigit;
        init(maxLabelLength);
    }

    protected LabelSequenceArray(int numOfBitsForEachDigit, int numOfDigits, int maxLabelLength) {
        labelSequenceArray = new LabelSequence[maxLabelLength][];
        labelSequence2MinimumRepeat = new MinimumRepeat[maxLabelLength][];
        markings = new IntOpenHashSet[maxLabelLength][];
        weights = new int[maxLabelLength];
        this.numOfBitsForEachDigit = numOfBitsForEachDigit;
        this.numOfDigits = numOfDigits;
        init(maxLabelLength);
    }

    private void init(int maxLabelLength) {
        int size = numOfDigits;
        for (int i = 0; i < maxLabelLength; i++) {
            labelSequenceArray[i] = new LabelSequence[size];
            labelSequence2MinimumRepeat[i] = new MinimumRepeat[size];
            markings[i] = new IntOpenHashSet[size];
            size = size << numOfBitsForEachDigit;
        }
        initWeights();
    }

    private void initWeights() {
        int w = 1;
        for (int i = 0; i < weights.length; i++) {
            weights[i] = w;
            w = w << numOfBitsForEachDigit;
        }
    }

    @Override
    public LabelSequence getEpsilon() {
        return EPSILON;
    }

    @Override
    public LabelSequence forwardExpand(LabelSequence currentLabelSequence, int newEdgeLabel) {
        LabelSequence[] expandedLss = labelSequenceArray[currentLabelSequence.length()];
        int index = currentLabelSequence.incrementalCode * numOfDigits + newEdgeLabel;
        LabelSequence expanded;
        if ((expanded = expandedLss[index]) == null) {
            int[] ints = new int[currentLabelSequence.length() + 1];
            System.arraycopy(currentLabelSequence.sequence, 0, ints, 0, currentLabelSequence.length());
            ints[ints.length - 1] = newEdgeLabel;
            expanded = new LabelSequence(ints, index);
            expandedLss[index] = expanded;
        }
        return expanded;
    }

    @Override
    public LabelSequence backwardExpand(LabelSequence currentLabelSequence, int newEdgeLabel) {
        LabelSequence[] expandedLss = labelSequenceArray[currentLabelSequence.length()];
        int index = currentLabelSequence.incrementalCode + newEdgeLabel * weights[currentLabelSequence.length()];
        LabelSequence expanded;
        if ((expanded = expandedLss[index]) == null) {
            int[] ints = new int[currentLabelSequence.length() + 1];
            System.arraycopy(currentLabelSequence.sequence, 0, ints, 1, currentLabelSequence.length());
            ints[0] = newEdgeLabel;
            expanded = new LabelSequence(ints, index);
            expandedLss[index] = expanded;
        }
        return expanded;
    }

    @Override
    public MinimumRepeat getMr(LabelSequence labelSequence) {
        MinimumRepeat mr;
        if ((mr = labelSequence2MinimumRepeat[labelSequence.length() - 1][labelSequence.incrementalCode]) == null) {
            mr = MinimumRepeat.computeMinimumRepeat(labelSequence.sequence);
            labelSequence2MinimumRepeat[labelSequence.length() - 1][labelSequence.incrementalCode] = mr;
        }
        return mr;
    }

    @Override
    public boolean isVisited(int vId, LabelSequence labelSequence) {
        IntOpenHashSet intOpenHashSet;
        if ((intOpenHashSet = markings[labelSequence.length() - 1][labelSequence.incrementalCode]) == null) {
            intOpenHashSet = new IntOpenHashSet();
            markings[labelSequence.length() - 1][labelSequence.incrementalCode] = intOpenHashSet;
        }
        return (!intOpenHashSet.add(vId));
    }

    @Override
    public void clearMarks() {
        for (IntOpenHashSet[] sets : markings)
            for (int i = 0, j = sets.length; i < j; i++)
                if (sets[i] != null)
                    sets[i] = null;
    }
}
