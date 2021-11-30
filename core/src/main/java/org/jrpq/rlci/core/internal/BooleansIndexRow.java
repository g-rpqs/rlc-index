package org.jrpq.rlci.core.internal;

import java.util.Arrays;
import java.util.BitSet;

public class BooleansIndexRow extends IndexRow {

    private static final long serialVersionUID = 2556891315369232980L;
    private boolean[] isTs;

    @Override
    BitSetIndexRow transform() {
        int size = getSize();
        BitSet bitSet = new BitSet(size);
        for (int i = 0; i < size; i++)
            if (isTs[i])
                bitSet.set(i);
        return new BitSetIndexRow(getUIds(), getMrs(), size, bitSet);
    }

    @Override
    void growIsT(int newLength) {
        isTs = Arrays.copyOf(isTs, newLength);
    }

    @Override
    boolean getIsT(int index) {
        return isTs[index];
    }

    @Override
    void setIsT(int index) {
        isTs[index] = true;
    }

    @Override
    void initIsT() {
        isTs = new boolean[DEFAULT_SIZE];
    }
}
