package org.jrpq.rlci.core.internal;

import java.util.BitSet;

public class BitSetIndexRow extends IndexRow {
    private static final long serialVersionUID = -2458213815755216007L;
    private BitSet isTs;

    public BitSetIndexRow(int[] uIds, int[] mrs, int size, BitSet isTs) {
        super(uIds, mrs, size);
        this.isTs = isTs;
    }

    @Override
    BitSetIndexRow transform() {
        return this;
    }

    @Override
    void growIsT(int newLength) {
    }

    @Override
    boolean getIsT(int index) {
        return isTs.get(index);
    }

    @Override
    void setIsT(int index) {
        isTs.set(index);
    }

    @Override
    void initIsT() {
        isTs = new BitSet();
    }
}
