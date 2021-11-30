package org.jrpq.rlci.benchmark.util.generators;

import java.util.Arrays;
import java.util.Objects;

public class Query<V> {
    private final V source;
    private final V target;
    private final String[] labelConstraint;
    private final boolean isTrue;
    private final Integer distance;

    public Query(V source, V target, String[] labelConstraint, boolean isTrue, Integer distance) {
        this.source = source;
        this.target = target;
        this.labelConstraint = labelConstraint;
        this.isTrue = isTrue;
        this.distance = distance;
    }

    public V getSource() {
        return source;
    }

    public V getTarget() {
        return target;
    }

    public String[] getLabelConstraint() {
        return labelConstraint;
    }

    public boolean isTrue() {
        return isTrue;
    }

    public Integer getDistance() {
        return distance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        org.jrpq.rlci.benchmark.util.generators.Query<?> query = (org.jrpq.rlci.benchmark.util.generators.Query<?>) o;
        return isTrue == query.isTrue && Objects.equals(source, query.source) && Objects.equals(target, query.target) && Arrays.equals(labelConstraint, query.labelConstraint);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(source, target, isTrue);
        result = 31 * result + Arrays.hashCode(labelConstraint);
        return result;
    }

    @Override
    public String toString() {
        return "Query{" +
                "source=" + source +
                ", target=" + target +
                ", labelConstraint=" + Arrays.toString(labelConstraint) +
                ", isTrue=" + isTrue +
                ", distance=" + distance +
                '}';
    }
}
