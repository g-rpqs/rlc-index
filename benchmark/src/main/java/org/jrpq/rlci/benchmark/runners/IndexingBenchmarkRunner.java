package org.jrpq.rlci.benchmark.runners;

import com.google.gson.Gson;
import org.jrpq.rlci.benchmark.util.EdgeList;
import org.jrpq.rlci.benchmark.baselines.ExtendedTransitiveClosure;
import org.jrpq.rlci.benchmark.util.config.BenchmarkConfig;
import org.jrpq.rlci.core.RlcIndex;
import org.jrpq.rlci.core.graphs.EdgeLabeledGraph;
import org.jrpq.rlci.core.graphs.RelationshipEdge;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.longs.LongLongImmutablePair;
import it.unimi.dsi.fastutil.longs.LongLongPair;
import org.apache.commons.csv.CSVPrinter;
import org.openjdk.jol.info.GraphLayout;

import java.io.*;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

import static org.jrpq.rlci.benchmark.runners.WorkloadBenchmarkRunner.*;

public class IndexingBenchmarkRunner {

    static final String BENCHMARK_DIR = "./benchmarks";

    public static void run(String pathToConfig) {
        Gson gson = new Gson();
        Reader reader = null;
        try {
            reader = new FileReader(pathToConfig);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.out.println("Invalid path of benchmark config file.");
        }
        assert reader != null;
        BenchmarkConfig benchmarkConfig = gson.fromJson(reader, BenchmarkConfig.class);
        System.out.println("Start experimentS with configuration file: " + pathToConfig);
        executeWithConfig(benchmarkConfig);
    }

    public static void runScalabilityExpWithK() {// k = 2,3,4
        executeScalabilityExpWithK(
                "./benchmark-results/standalone-results/synthetic-graphs/indexing-results/er-zipf/various-k",
                BENCHMARK_DIR + "/datasets/synthetic-graphs/er-zipf/125000/er-125000-625000-16.txt"
        );
        executeScalabilityExpWithK(
                "./benchmark-results/standalone-results/synthetic-graphs/indexing-results/ba-zipf/various-k",
                BENCHMARK_DIR + "/datasets/synthetic-graphs/ba-zipf/125000/ba-125000-625000-16.txt"
        );
    }

    private static void executeScalabilityExpWithK(String pathToWrite, String pathToGraph) {
        String graphName = Paths.get(pathToGraph).getFileName().toString().replace(".txt", "");
        System.out.println("Start k-scalability experiments for " + graphName);
        int repeatOfExecution = 5, duration = 60 * 6;
        int[] paras = new int[]{2, 3, 4};
        final EdgeLabeledGraph<Integer, RelationshipEdge> graph = EdgeList.read(pathToGraph, graphName);
        String[] header = {"graph", "method", "k", "indexing_time_ns", "index_size_b"};
        CSVPrinter csvPrinter = WorkloadBenchmarkRunner.getCSVPrinter(pathToWrite, header);
        if (csvPrinter == null)
            return;
        for (int k : paras) {
            for (int i = 0; i < repeatOfExecution; i++) {
                Pair<LongLongPair, RlcIndex<Integer, RelationshipEdge>> pair = getRlcIndexingBenchmarkResult(graph, k, duration);
                LongLongPair timeAndSize = pair.first();
                if (timeAndSize == null)
                    continue;
                if (timeAndSize.firstLong() == Long.MAX_VALUE) { // time out
                    System.out.println("Time out for graph " + graphName + ", k = " + k);
                    break;
                }
                try {
                    csvPrinter.printRecord(graphName, CSV_COLUMN_NAME_RLC, k, timeAndSize.firstLong(), timeAndSize.secondLong());
                    csvPrinter.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("The following result cannot be recorded: " + graphName + ", " + CSV_COLUMN_NAME_RLC + ", " + timeAndSize.firstLong() + ", " + timeAndSize.secondLong());
                    break;
                }
//                if (i == repeatOfExecution - 1 && pair.right() != null) { // serialization of index
//                    try {
//                        RlcIndex.serialize(pair.second(), PATH_TO_PREBUILT_RLC_INDEX_DIR + "/index/synthetic-graphs/er-zipf/various-k/" + java.time.LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + "-" + graphName + "-k=" + k + ".txt");
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
            }
        }
        try {
            csvPrinter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void executeWithConfig(final BenchmarkConfig benchmarkConfig) {
        Set<String> skippedGraphs = new HashSet<>(Arrays.asList(benchmarkConfig.getSkippedGraphNames()));
        String[] header = {"graph", "method", "indexing_time_ns", "index_size_b"};
        CSVPrinter csvPrinter = WorkloadBenchmarkRunner.getCSVPrinter(benchmarkConfig.getPathToWrite(), header);
        if (csvPrinter == null)
            return;
        for (String method : benchmarkConfig.getMethods()) {
            if (method == null)
                continue;
            for (String pathToGraph : EdgeList.listPaths(benchmarkConfig.getPathToGraphRootDirectory())) {
                final String graphName = Paths.get(pathToGraph).getFileName().toString().replace(".txt", "");
                if (skippedGraphs.contains(graphName))
                    continue;
                final EdgeLabeledGraph<Integer, RelationshipEdge> graph = EdgeList.read(pathToGraph, graphName);
                System.out.println("Current method: " + method);
                for (int i = 0; i < benchmarkConfig.getRepeatOfExecution(); i++) {
                    LongLongPair timeAndSize = getTimeAndSize(graph, method, benchmarkConfig.getTimeOutDurationInMinutes());
                    if (timeAndSize == null)
                        continue;
                    if (timeAndSize.firstLong() == Long.MAX_VALUE) // time out
                        break;
                    try {
                        csvPrinter.printRecord(graphName, method, timeAndSize.firstLong(), timeAndSize.secondLong());
                        csvPrinter.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.out.println("The following result cannot be recorded: " + graphName + ", " + method + ", " + timeAndSize.firstLong() + ", " + timeAndSize.secondLong());
                        break;
                    }
                }
            }
        }
        try {
            csvPrinter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Pair<LongLongPair, RlcIndex<Integer, RelationshipEdge>> getRlcIndexingBenchmarkResult(EdgeLabeledGraph<Integer, RelationshipEdge> graph, int k, long duration) {
        long start, end;
        boolean isTo;
        RlcIndex<Integer, RelationshipEdge> rlcIndex = new RlcIndex<>(graph, k);
        start = System.nanoTime();
        isTo = runWithTimeOut(duration, rlcIndex::build);
        end = System.nanoTime();
        if (isTo) {
            System.out.println("Time out!!!");
            return Pair.of(LongLongImmutablePair.of(Long.MAX_VALUE, Long.MAX_VALUE), null);
        } else {
            long size = GraphLayout.parseInstance(rlcIndex.getIndex()).totalSize();
            System.out.println(size);
            return Pair.of(LongLongImmutablePair.of(end - start, size), rlcIndex);
        }
    }

    private static LongLongPair getTimeAndSize(EdgeLabeledGraph<Integer, RelationshipEdge> graph, String method, long duration) {
        switch (method) {
            case CSV_COLUMN_NAME_RLC:
                return getRlcIndexingBenchmarkResult(graph, 2, duration).first();
            case CSV_COLUMN_NAME_ETC:
                long start, end;
                ExtendedTransitiveClosure<Integer, RelationshipEdge> extendedTransitiveClosure = new ExtendedTransitiveClosure<>(graph, 2);
                start = System.nanoTime();
                boolean isTO = runWithTimeOut(duration, extendedTransitiveClosure::build);
                end = System.nanoTime();
                if (isTO) {
                    System.out.println("Time out!!! Graph: " + graph.getGraphName());
                    return LongLongImmutablePair.of(Long.MAX_VALUE, Long.MAX_VALUE);
                } else {
//                    try { //
//                        ExtendedTransitiveClosure.serialize(extendedTransitiveClosure, "./kl-index-benchmark/extended-transitive-closure/" + graph.getGraphName() + ".etc" );
//                    } catch (IOException e) {
//                        System.out.println("Serialization failed");
//                        e.printStackTrace();
//                    }
                    return LongLongImmutablePair.of(end - start, GraphLayout.parseInstance(extendedTransitiveClosure.getTC()).totalSize());
                }
            default:
                System.out.println("Invalid method!!!");
                return null;
        }
    }

    public static boolean runWithTimeOut(long durationInMinutes, Runnable task) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(task);
        boolean isTO = false;
        try {
            long start = System.nanoTime();
            future.get(durationInMinutes, TimeUnit.MINUTES);
            long end = System.nanoTime();
            System.out.println("Task time: " + (end - start));
            //task successful
        } catch (TimeoutException e) {
            //timeout
            future.cancel(true);
            isTO = true;
        } catch (InterruptedException e) {
            //current thread was interrupted during task execution
            Thread.currentThread().interrupt();
        } catch (OutOfMemoryError | ExecutionException throwable) {
            throwable.printStackTrace();
        } //task threw Exception
        finally {
            executor.shutdownNow();
        }
        return isTO;
    }
}
