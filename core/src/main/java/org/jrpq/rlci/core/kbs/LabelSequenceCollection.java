package org.jrpq.rlci.core.kbs;

public abstract class LabelSequenceCollection {

    public static LabelSequenceCollection createInstance(int numOfLabels, int k) {
        LabelSequenceCollection labelSequenceCollection;
        double log2OfNumOfLabels = Math.log(numOfLabels) / Math.log(2);
        int numOfBits = Math.floor(log2OfNumOfLabels) == log2OfNumOfLabels ? (int) (log2OfNumOfLabels) : (int) (Math.floor(log2OfNumOfLabels) + 1);
        int numOfDigits = 1 << numOfBits; // numOfDigits = 2 ^ numBits
        int totalObjects = (int) ((Math.pow(numOfDigits, k + 1) - numOfDigits) / (numOfDigits - 1)); // \sum_{i=1}^{k} numOfDigits^i
        int requiredBytes = totalObjects * 4; // each reference requires 4 bytes
        long threshold = Runtime.getRuntime().totalMemory() / 50;
        if (requiredBytes <= threshold) {
            labelSequenceCollection = new LabelSequenceArray(numOfBits, numOfDigits, k);
            System.out.println("Create label sequence array");
        } else {
            labelSequenceCollection = new LabelSequenceHashMap(k);
            System.out.println("Create label sequence hashmap");
        }
        return labelSequenceCollection;
    }

    public abstract LabelSequence getEpsilon();

    public abstract LabelSequence forwardExpand(LabelSequence currentLabelSequence, int newEdgeLabel);

    public abstract LabelSequence backwardExpand(LabelSequence currentLabelSequence, int newEdgeLabel);

    public abstract MinimumRepeat getMr(LabelSequence labelSequence);

    public abstract boolean isVisited(int vId, LabelSequence labelSequence);

    public abstract void clearMarks();
}
