package org.jrpq.rlci.demo;

import org.jrpq.rlci.benchmark.util.EdgeList;
import org.jrpq.rlci.benchmark.util.generators.Query;
import org.jrpq.rlci.benchmark.util.generators.WorkLoadGenerator;
import org.jrpq.rlci.core.RlcIndex;
import org.jrpq.rlci.core.graphs.EdgeLabeledGraph;
import org.jrpq.rlci.core.graphs.RelationshipEdge;
import org.openjdk.jol.info.GraphLayout;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;


public class RlcIndexDemo {

    public static void main(String[] args) throws IOException {
        String pathToAdGraph = "./demo-data/01-advogato.txt",
                pathToAdWorkloadDir = "./demo-data";
        EdgeLabeledGraph<Integer, RelationshipEdge> edgeLabeledGraph = EdgeList.read(pathToAdGraph, "01-advogato");

        RlcIndex<Integer, RelationshipEdge> rlcIndex = new RlcIndex<>(edgeLabeledGraph, 2);
        long start = System.nanoTime();
        rlcIndex.build();
        long end = System.nanoTime();
        System.out.println("Time of building the RLC index for the AD graph: " + (end - start) + " nanoseconds.");
        System.out.println("The size of the RLC index for the AD graph: " + GraphLayout.parseInstance(rlcIndex.getIndex()).totalSize() + " bytes.");

        List<String> paths = Files.walk(Path.of(pathToAdWorkloadDir))
                .filter(Files::isRegularFile)
                .map(Path::toString)
                .filter(s -> s.contains("01-advogato") && s.contains(".json"))
                .collect(Collectors.toList());

        System.out.println("Start workload execution.");
        for (String workloadPath : paths) {
            List<Query<Integer>> queryList = WorkLoadGenerator.readQueryListFromJsonFile(workloadPath);

            for (Query<Integer> query : queryList)
                rlcIndex.query(query.getSource(), query.getTarget(), query.getLabelConstraint());
        }
        System.out.println("Done.");
    }
}
