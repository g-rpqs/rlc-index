package org.jrpq.rlci.benchmark;

import org.jrpq.rlci.benchmark.runners.ExtendedQueryBenchmarkRunner;
import org.jrpq.rlci.benchmark.runners.IndexingBenchmarkRunner;
import org.jrpq.rlci.benchmark.runners.WorkloadBenchmarkRunner;

public class App {
    private static final String
            PATH_TO_INDEXING_BENCHMARK_CONFIG = "./configurations/real-graphs-indexing.json",
            PATH_TO_QUERYING_BENCHMARK_CONFIG = "./configurations/real-graphs-querying.json",
//            PATH_TO_RLC_PREBUILT_INDEX_QUERYING_BENCHMARK_CONFIG = "./configurations/real-graphs-querying-prebuilt-index.json",
            PATH_TO_SYNTHETIC_GRAPHS_INDEXING_CONFIG = "./configurations/synthetic-graphs-indexing.json",
            PATH_TO_SYNTHETIC_GRAPHS_QUERYING_CONFIG = "./configurations/synthetic-graphs-querying.json";

    public static void main(String[] args) {
        runExperimentsUsingRealGraphs(); // Experiments in Section 5.1

         runExperimentsUsingSyntheticGraphs(); // Experiments in Section 5.2

         ExtendedQueryBenchmarkRunner.run(3); // Experiments in section 5.3
    }

    private static void runExperimentsUsingRealGraphs() {
        // Experiments of building the ETC and the RLC index for real graphs
        IndexingBenchmarkRunner.run(PATH_TO_INDEXING_BENCHMARK_CONFIG);

        // Experiments of workload execution time using Bfs, BiBfs, ETC, RlcIndex
        WorkloadBenchmarkRunner.runBaselineExecutionTimeBenchmarkWithConfig(PATH_TO_QUERYING_BENCHMARK_CONFIG);

        // Uncomment the following line for running query time experiment using prebuilt RLC indices for real graphs
//         WorkloadBenchmarkRunner.runRlcPrebuiltIndexExecutionTimeBenchmarkWithConfig(false, PATH_TO_RLC_PREBUILT_INDEX_QUERYING_BENCHMARK_CONFIG);
    }

    private static void runExperimentsUsingSyntheticGraphs() {
        // Experiments of building the RLC index for synthetic graphs with varying characteristics: label set size, average degree, and number of vertices
        IndexingBenchmarkRunner.run(PATH_TO_SYNTHETIC_GRAPHS_INDEXING_CONFIG);
        // Experiments of workload execution time using the RLC index for synthetic graphs with varying characteristics: label set size, average degree, and number of vertices
        WorkloadBenchmarkRunner.runBaselineExecutionTimeBenchmarkWithConfig(PATH_TO_SYNTHETIC_GRAPHS_QUERYING_CONFIG);

        // Experiments of building the RLC index using varying k
        IndexingBenchmarkRunner.runScalabilityExpWithK();
        // Experiments of workload execution time using RLC indices with various k
        WorkloadBenchmarkRunner.runScalabilityExpWithK();
    }
}
