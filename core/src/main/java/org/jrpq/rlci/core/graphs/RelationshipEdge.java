package org.jrpq.rlci.core.graphs;

import org.jgrapht.graph.DefaultEdge;

// https://github.com/jgrapht/jgrapht/blob/master/jgrapht-demo/src/main/java/org/jgrapht/demo/LabeledEdges.java
// The hashCode() and equals() are not overridden, such that every edge is different.
public class RelationshipEdge extends DefaultEdge {
    private static final long serialVersionUID = -6022237394448737459L;
    private final int label;

    /**
     * Constructs a relationship edge
     *
     * @param label the label of the new edge.
     */
    public RelationshipEdge(int label) {
        this.label = label;
    }

    /**
     * Gets the label associated with this edge.
     *
     * @return edge label
     */
    public int getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return "(" + getSource() + " : " + getTarget() + " : " + label + ")";
    }
}
