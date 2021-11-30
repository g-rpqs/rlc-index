package org.jrpq.rlci.benchmark.util.generators;

import java.util.Arrays;

public class ExtendedQuery<V> extends Query<V> {
    private final int typeId;
    // This object represent different types of queries explained below:
    // If typeId = 3, the query is of type Q3, i.e., ab+, and the labelConstraint = {"a", "b"}
    // If typeId = 4, the query is of type Q4, i.e., ab+c, and the labelConstraint = {"a", "b", "c"}
    // If typeId = 5, the query is of type Q5, i.e., a+b+, and the labelConstraint = {"a", "b"}
    // If typeId = 6, the query is of type Q6, i.e., ab+c+, and the labelConstraint = {"a", "b", "c"}


    public ExtendedQuery(int typeId, V source, V target, String[] labelConstraint, boolean isTrue, Integer distance) {
        super(source, target, labelConstraint, isTrue, distance);
        this.typeId = typeId;
    }

    public int getTypeId() {
        return typeId;
    }

    @Override
    public boolean equals(Object o) {
        org.jrpq.rlci.benchmark.util.generators.ExtendedQuery<?> query = (org.jrpq.rlci.benchmark.util.generators.ExtendedQuery<?>) o;
        return typeId == ((org.jrpq.rlci.benchmark.util.generators.ExtendedQuery<?>) o).typeId && super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode() * 31 + Integer.hashCode(typeId);
    }

    @Override
    public String toString() {
        return "ExtendedQuery{" +
                "typeId=" + typeId +
                ",source=" + super.getSource() +
                ", target=" + super.getTarget() +
                ", labelConstraint=" + Arrays.toString(super.getLabelConstraint()) +
                ", isTrue=" + super.isTrue() +
                ", distance=" + super.getDistance() +
                '}';
    }
}
