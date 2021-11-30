package org.jrpq.rlci.core.kbs;

import java.io.Serializable;
import java.util.Arrays;

public final class MinimumRepeat implements Serializable {

    private static final long serialVersionUID = 4105117408478850432L;
    private final int[] mr;
    private final int hashCode;

    private MinimumRepeat(int[] mr) {
        this.mr = mr;
        this.hashCode = Arrays.hashCode(mr);
    }

    // Failure function explanation: https://cs.brown.edu/courses/csci1810/lectures/slides/failure_function.pdf
    // Let W be a search word.
    // The failure function of W is an array of length |W|.
    // The i-element (i starts with 0) in the failure function stores the longest proper suffix of the string (W[0],...,W[i]) that is also a proper prefix of W.
    private static void buildFailureFunction(final int[] pattern, int[] failureFunction) {
        int j = 0;
        int i = 1;
        failureFunction[0] = 0;
        int length = pattern.length;
        while (i < length) {
            if (pattern[i] == pattern[j]) {
                j++;
                failureFunction[i] = j;
                i++;
            } else {
                if (j != 0) {
                    j = failureFunction[j - 1];
                } else {
                    failureFunction[i] = j;
                    i++;
                }
            }
        }
    }

    public static MinimumRepeat computeMinimumRepeat(int[] labelSequence) {
        if (labelSequence == null)
            return null;
        int length = labelSequence.length;
        int[] f = new int[length];
        buildFailureFunction(labelSequence, f);
        boolean isLabelSequence = false;
        int lengthOfMR = labelSequence.length - f[f.length - 1];
        int[] minimumRepeat = new int[lengthOfMR];
        if (labelSequence.length % lengthOfMR == 0) {
            System.arraycopy(labelSequence, 0, minimumRepeat, 0, lengthOfMR);
            int j = 0;
            for (int i = lengthOfMR; i < length; i++) {
                if (minimumRepeat[j++] != labelSequence[i]) {
                    isLabelSequence = true;
                    break;
                }
                if (j == lengthOfMR)
                    j = 0;
            }
        } else
            isLabelSequence = true;
        return new MinimumRepeat((isLabelSequence) ? labelSequence : minimumRepeat);
    }

    // Check whether the labelSequence contains a 'potential prefix' of the constraints
    // Return -1 if not, otherwise return the index of the last element in the real prefix
    // For example, constraint = ab,
    // if outMR is ababa, return the index of a, i.e., 0
    // if outMR is ababb, return -1
    @Deprecated
    public static int prefixCheck(MinimumRepeat outMR, MinimumRepeat constraints) {
        return prefixCheck(outMR.mr, constraints.mr);
    }

    @Deprecated
    public static int prefixCheck(int[] outMR, int[] constraints) {
        int lengthOfOutMR = outMR.length, lengthOfConstraints = constraints.length;
        int a = outMR.length / constraints.length;
        int b = outMR.length % constraints.length;
        if (a == 0) {
            for (int i = 0; i < lengthOfOutMR; i++)
                if (outMR[i] != constraints[i])
                    return -1;
            return lengthOfOutMR - 1;
        } else {
            int offset = 0;
            for (int i = 0; i < a; i++) {
                for (int j = 0; j < lengthOfConstraints; j++)
                    if (outMR[offset + j] != constraints[j])
                        return -1;
                offset += lengthOfConstraints;
            }
            if (b == 0) {
                return lengthOfConstraints - 1;
            } else {
                for (int i = 0; i < b; i++)
                    if (outMR[offset + i] != constraints[i])
                        return -1;
                return b - 1;
            }
        }
    }

    // Check whether the labelSequence contains a 'potential suffix' of the constraints
    // Return -1 if not, otherwise return the first real suffix
    // For example, constraint = ab,
    // if outMR is babab, return the index of b, i.e., 1
    // if outMR is aabab, return -1
    @Deprecated
    public static int suffixCheck(MinimumRepeat inMR, MinimumRepeat constraints) {
        return suffixCheck(inMR.mr, constraints.mr);
    }

    @Deprecated
    public static int suffixCheck(int[] inMR, int[] constraints) {
        int lengthOfOutMR = inMR.length, lengthOfConstraints = constraints.length;
        int a = inMR.length / constraints.length;
        int b = inMR.length % constraints.length;
        if (a == 0) {
            int offset = lengthOfConstraints - 1;
            for (int i = lengthOfOutMR - 1; i > -1; i--)
                if (inMR[i] != constraints[offset--])
                    return -1;
            return lengthOfConstraints - lengthOfOutMR;
        } else {
            int offset = b;
            for (int i = 0; i < a; i++) {
                for (int j = 0; j < lengthOfConstraints; j++)
                    if (inMR[offset + j] != constraints[j])
                        return -1;
                offset += lengthOfConstraints;
            }
            if (b == 0) {
                return 0;
            } else {
                offset = lengthOfConstraints - b;
                for (int i = 0; i < b; i++)
                    if (inMR[i] != constraints[i + offset])
                        return -1;
                return lengthOfConstraints - b;
            }
        }
    }

    public static boolean checkConcatenation(int[] prefix, int[] suffix, int[] constraints) {
        int j = 0;
        for (int b : prefix)
            if (b != constraints[j++])
                return false;
        for (int b : suffix)
            if (b != constraints[j++])
                return false;
        return true;
    }

    public int length() {
        return mr.length;
    }

    public int getState(int stateId) {
        return mr[stateId];
    }

    public int[] getMrView(){
        return Arrays.copyOf(mr, mr.length);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MinimumRepeat))
            return false;
        return Arrays.equals(mr, ((MinimumRepeat) obj).mr);
    }

    @Override
    public String toString() {
        return Arrays.toString(mr);
    }
}
