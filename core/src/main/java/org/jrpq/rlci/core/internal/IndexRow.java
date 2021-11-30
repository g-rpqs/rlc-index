package org.jrpq.rlci.core.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class IndexRow implements Serializable {
    static final IntIntPair REUSED_PAIR = new IntIntPair(0, 0);
    static final int DEFAULT_SIZE = 2, THRESHOLD_TO_TRANSFORM = 33;
    private static final long serialVersionUID = -1379381303538363577L;
    private int[] uIds;
    private int[] mrs;
    private int size;

    public IndexRow() {
        uIds = new int[DEFAULT_SIZE];
        mrs = new int[DEFAULT_SIZE];
        initIsT();
        size = 0;
    }

    public IndexRow(int[] uIds, int[] mrs, int size) {
        this.uIds = uIds;
        this.mrs = mrs;
        this.size = size;
    }

    public List<IntIntPair> getIndexRowView() {
        List<IntIntPair> list = new ArrayList<>();
        for (int i = 0; i < size; i++)
            list.add(new IntIntPair(uIds[i], mrs[i]));
        return list;
    }

    void add(int uId, int mr, boolean isT) {
        if (size == uIds.length)
            grow();
        uIds[size] = uId;
        mrs[size] = mr;
        if (isT)
            setIsT(size);
        size++;
    }

    // return -1 if the specified index entry is not contained in this indexRow
    int getIndexFromEnd(int uId, int mr) {
        int index = size - 1;
        while (index > -1 && uIds[index] == uId) {
            if (mrs[index] == mr)
                return index;
            index--;
        }
        return -1;
    }

    IntIntPair getTheRangeOf(int uId) {
        // uIds is already sorted
        // it is not interesting to change the binary search to get the first occurrence of uId
        // because the number of duplicates of uId is not high compared to the length of the array uIDs
        int start = binarySearch(uId);
        int end = start;
        if (start < 0)
            return null;
        start--;
        while (start > -1 && uIds[start] == uId)
            start--;
        REUSED_PAIR.first = start + 1;
        int len = size;
        end++;
        while (end < len && uIds[end] == uId)
            end++;
        REUSED_PAIR.second = end;
        return REUSED_PAIR;
    }

    private int binarySearch(int key) {
        int low = 0;
        int high = size - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int midVal = uIds[mid];
            if (midVal < key)
                low = mid + 1;
            else if (midVal > key)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -1;  // key not found.
    }

    boolean contains(int from, int to, int mr) {
        for (int i = from; i < to; i++)
            if (mrs[i] == mr)
                return true;
        return false;
    }

    private void grow() {
        int oldLength = uIds.length;
        int newLength = oldLength + (oldLength >> 1);
        uIds = Arrays.copyOf(uIds, newLength);
        mrs = Arrays.copyOf(mrs, newLength);
        growIsT(newLength);
    }

    int getSize() {
        return size;
    }

    int getMr(int index) {
        return mrs[index];
    }

    abstract BitSetIndexRow transform();

    abstract void growIsT(int newLength);

    abstract boolean getIsT(int index);

    abstract void setIsT(int index);

    abstract void initIsT();

    public int[] getUIds() {
        return uIds;
    }

    public int[] getMrs() {
        return mrs;
    }

    static class IntIntPair {
        int first, second;

        IntIntPair(int first, int second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public String toString() {
            return "(" + first + ", " + second + ")";
        }
    }
}
