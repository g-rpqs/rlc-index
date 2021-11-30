package org.jrpq.rlci.benchmark.runners;

import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jrpq.rlci.benchmark.util.EdgeList;
import org.jrpq.rlci.benchmark.util.generators.ExtendedQuery;
import org.jrpq.rlci.benchmark.util.generators.WorkLoadGenerator;
import org.jrpq.rlci.core.ExtendedQuerySupport;
import org.jrpq.rlci.core.RlcIndex;
import org.jrpq.rlci.core.graphs.EdgeLabeledGraph;
import org.jrpq.rlci.core.graphs.RelationshipEdge;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.jrpq.rlci.benchmark.runners.IndexingBenchmarkRunner.BENCHMARK_DIR;
import static org.jrpq.rlci.benchmark.runners.WorkloadBenchmarkRunner.CSV_COLUMN_NAME_RLC;
import static org.jrpq.rlci.benchmark.runners.WorkloadBenchmarkRunner.PATH_TO_PREBUILT_RLC_INDEX_DIR;

public class ExtendedQueryBenchmarkRunner {
    public static void run(int k) {
        System.out.println("Extended query execution experiments with k = " + k);
        String graphName = "04-web-NotreDame";

        String pathToGraphRoot = BENCHMARK_DIR + "/datasets/real-world-graphs";
        String pathToExtendedQueryDir = BENCHMARK_DIR + "/workloads/real-world-graphs/extended-queries";
        CSVPrinter csvPrinter = WorkloadBenchmarkRunner.getCSVPrinter("./benchmark-results/extended-query-results", WorkloadBenchmarkRunner.HEADER_TOTAL_QUERY_TIME);

        EdgeLabeledGraph<Integer, RelationshipEdge> edgeLabeledGraph = EdgeList.read(pathToGraphRoot + File.separator + graphName + ".txt", graphName);
        RlcIndex<Integer, RelationshipEdge> rlcIndex;
        if (k == 2) {
            try {
                rlcIndex = RlcIndex.deserialize(edgeLabeledGraph, PATH_TO_PREBUILT_RLC_INDEX_DIR + "/real-world-graphs/k-2/04-web-NotreDame.rlci");
                System.out.println("The RLC index with k = " + k + " has been loaded.");
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                System.out.println("The RLC index cannot be loaded");
                System.out.println("Start building the RLC index.");
                rlcIndex = new RlcIndex<>(edgeLabeledGraph, 2);
                rlcIndex.build();
            }
        } else if (k == 3) {
            try {
                rlcIndex = RlcIndex.deserialize(edgeLabeledGraph, PATH_TO_PREBUILT_RLC_INDEX_DIR + "/real-world-graphs/k-3/Web-notre-dame-k-3.rlci");
                System.out.println("The RLC index with k = " + k + " has been loaded.");
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                System.out.println("The RLC index cannot be loaded");
                System.out.println("Start building the RLC index.");
                rlcIndex = new RlcIndex<>(edgeLabeledGraph, 3);
                rlcIndex.build();
            }
        } else {
            System.out.println("The RLC index for the WN graph with k = " + k + " has not been built yet.");
            return;
        }
        System.out.println("Start experiments.");
        List<ExtendedQuery<Integer>> extendedQueryList = WorkLoadGenerator.readQueryListFromJsonFile2(pathToExtendedQueryDir + File.separator + graphName + "-extended-queries.json");
        for (ExtendedQuery<Integer> query : extendedQueryList) {
            if (k == 2 && query.getTypeId() == 0) // skip (abc)+ if k = 2
                continue;
            System.out.println("Type id: " + query.getTypeId());
            System.out.println(query);
            int repeat = 20; // It would be better to use a larger value for the RLC index as the query time is at the level of a few microseconds.
            DescriptiveStatistics descriptiveStatistics = new DescriptiveStatistics();
            switch (query.getTypeId()) {
                case 0: // (abc)+
                case 1: // (ab)+
                case 2: // (a)+
                    for (int i = 0; i < repeat; i++) {
                        long start = System.nanoTime();
                        rlcIndex.query(query.getSource(), query.getTarget(), query.getLabelConstraint());
                        long end = System.nanoTime();
                        descriptiveStatistics.addValue(end - start);
                    }
                    break;
                case 3:
                    for (int i = 0; i < repeat; i++) {
                        long start = System.nanoTime();
                        ExtendedQuerySupport.queryQ3(rlcIndex, query.getSource(), query.getTarget(), query.getLabelConstraint()[0], query.getLabelConstraint()[1]);
                        long end = System.nanoTime();
                        descriptiveStatistics.addValue(end - start);
                    }
                    break;
                case 4:
                    for (int i = 0; i < repeat; i++) {
                        long start = System.nanoTime();
                        ExtendedQuerySupport.queryQ4(rlcIndex, query.getSource(), query.getTarget(), query.getLabelConstraint()[0], query.getLabelConstraint()[1], query.getLabelConstraint()[2]);
                        long end = System.nanoTime();
                        descriptiveStatistics.addValue(end - start);
                    }
                    break;
                case 5:
                    for (int i = 0; i < repeat; i++) {
                        long start = System.nanoTime();
                        ExtendedQuerySupport.queryQ5(rlcIndex, query.getSource(), query.getTarget(), query.getLabelConstraint()[0], query.getLabelConstraint()[1]);
                        long end = System.nanoTime();
                        descriptiveStatistics.addValue(end - start);
                    }
                    break;
                case 6:
                    for (int i = 0; i < repeat; i++) {
                        long start = System.nanoTime();
                        ExtendedQuerySupport.queryQ6(rlcIndex, query.getSource(), query.getTarget(), query.getLabelConstraint()[0], query.getLabelConstraint()[1], query.getLabelConstraint()[2]);
                        long end = System.nanoTime();
                        descriptiveStatistics.addValue(end - start);
                    }
                    break;
                default:
                    break;
            }
            try {
                assert csvPrinter != null;
                csvPrinter.printRecord(graphName, query.getTypeId(), CSV_COLUMN_NAME_RLC, "true", descriptiveStatistics.getPercentile(50));
                csvPrinter.flush();
            } catch (IOException e) {
                System.out.println("Experimental results cannot be recorded!");
                System.out.println(graphName + ", " + query.getTypeId() + ", " + CSV_COLUMN_NAME_RLC + ", true, " + descriptiveStatistics.getPercentile(50));
                e.printStackTrace();
            }
        }

        try {
            assert csvPrinter != null;
            csvPrinter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
