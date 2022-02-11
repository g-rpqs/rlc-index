package org.jrpq.rlci.benchmark.runners;

import com.google.gson.Gson;
import org.jrpq.rlci.benchmark.util.EdgeList;
import org.jrpq.rlci.benchmark.util.generators.Query;
import org.jrpq.rlci.benchmark.baselines.ExtendedTransitiveClosure;
import org.jrpq.rlci.benchmark.baselines.OnlineTraversal;
import org.jrpq.rlci.benchmark.util.config.WorkloadBenchmarkConfig;
import org.jrpq.rlci.benchmark.util.generators.WorkLoadGenerator;
import org.jrpq.rlci.core.RlcIndex;
import org.jrpq.rlci.core.graphs.EdgeLabeledGraph;
import org.jrpq.rlci.core.graphs.RelationshipEdge;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.alg.util.Triple;
import org.openjdk.jol.info.GraphLayout;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.jrpq.rlci.benchmark.util.generators.WorkLoadGenerator.readQueryListsFromJsonFiles;
import static org.jrpq.rlci.benchmark.runners.IndexingBenchmarkRunner.BENCHMARK_DIR;
import static org.jrpq.rlci.benchmark.runners.IndexingBenchmarkRunner.runWithTimeOut;

public class WorkloadBenchmarkRunner {

    public static final String[] HEADER_TOTAL_QUERY_TIME = new String[]{"graph", "workload", "method", "query_type", "execution_time_ns"};
    public static final String[] HEADER_INDIVIDUAL_QUERY_TIME = new String[]{"graph", "workload", "method", "query_type", "query_id", "execution_time_ns"};
    static final int WARM_UP = 2;
    static final String PATH_TO_PREBUILT_RLC_INDEX_DIR = BENCHMARK_DIR + "/prebuilt-rlc-indices/";
    static final String CSV_COLUMN_NAME_RLC = "RlcIndex", CSV_COLUMN_NAME_ETC = "Etc", CSV_COLUMN_NAME_BFS = "Bfs", CSV_COLUMN_NAME_BIBFS = "BiBfs";

    public static void runRlcPrebuiltIndexExecutionTimeBenchmarkWithConfig(boolean recordIndividualQueryTime, String path) {
        String[] header = recordIndividualQueryTime ? HEADER_INDIVIDUAL_QUERY_TIME : HEADER_TOTAL_QUERY_TIME;
        Consumer<Triple<LongList, CSVPrinter, Object[]>> recording = recordIndividualQueryTime ? org.jrpq.rlci.benchmark.runners.WorkloadBenchmarkRunner::recordIndividualQueryExecution : org.jrpq.rlci.benchmark.runners.WorkloadBenchmarkRunner::recordTotalQueryExecution;
        Consumer<ExecutionConfig> executionConsumer = (executionConfig) -> executeQueryTimeBenchmarkWithPrebuiltIndex(recordIndividualQueryTime, executionConfig, recording);
        runExecutionTimeBenchmarkWithConfig(header, executionConsumer, path);
    }

    public static void runBaselineExecutionTimeBenchmarkWithConfig(String path) { // workload execution time
        Consumer<Triple<LongList, CSVPrinter, Object[]>> recording = org.jrpq.rlci.benchmark.runners.WorkloadBenchmarkRunner::recordTotalQueryExecution;
        Consumer<ExecutionConfig> executionConsumer = (executionConfig) -> executeExecutionTimeBenchmark(executionConfig, recording);
        runExecutionTimeBenchmarkWithConfig(HEADER_TOTAL_QUERY_TIME, executionConsumer, path);
    }

    // general method in benchmark framework
    public static void runExecutionTimeBenchmarkWithConfig(String[] header, Consumer<ExecutionConfig> benchmarkTask, String path) {
        Consumer<WorkloadBenchmarkConfig> workloadBenchmarkRunnerConsumer = workloadBenchmarkConfig -> openBenchmark(workloadBenchmarkConfig, benchmarkTask, header);
        executeBenchmarkWithConfig(workloadBenchmarkRunnerConsumer, path);
    }

    public static void runScalabilityExpWithK() {
        String pathToWrite = "./benchmark-results/standalone-results/synthetic-graphs/workload-results/final/various-k";
        CSVPrinter csvPrinter = getCSVPrinter(pathToWrite, HEADER_TOTAL_QUERY_TIME);
        executeScalabilityExpWithK(
                BENCHMARK_DIR + "/datasets/synthetic-graphs/er-zipf/125000/er-125000-625000-16.txt",
                PATH_TO_PREBUILT_RLC_INDEX_DIR + "/synthetic-graphs/er-zipf/various-k/er-125000-625000-16-k=",
                BENCHMARK_DIR + "/workloads/synthetic-graphs/er-zipf/n-5-16",
                csvPrinter
        );
        executeScalabilityExpWithK(
                BENCHMARK_DIR + "/datasets/synthetic-graphs/ba-zipf/125000/ba-125000-625000-16.txt",
                PATH_TO_PREBUILT_RLC_INDEX_DIR + "/synthetic-graphs/ba-zipf/various-k/ba-125000-625000-16-k=",
                BENCHMARK_DIR + "/workloads/synthetic-graphs/ba-zipf/n-5-16",
                csvPrinter
        );
        try {
            assert csvPrinter != null;
            csvPrinter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void executeScalabilityExpWithK(String pathToGraph, String pathToIndexDir, String pathToWorkloadRootDir, CSVPrinter csvPrinter) {
        String graphName = Paths.get(pathToGraph).getFileName().toString().replace(".txt", "");
        EdgeLabeledGraph<Integer, RelationshipEdge> edgeLabeledGraph = EdgeList.read(pathToGraph, graphName);
        List<String> workloadPaths = EdgeList.listPaths(pathToWorkloadRootDir).stream().filter(p -> p.contains(graphName) && p.contains(".json")).collect(Collectors.toList());
        for (int k = 2; k < 5; k++) {
            RlcIndex<Integer, RelationshipEdge> rlcIndex;
            try {
                rlcIndex = RlcIndex.deserialize(edgeLabeledGraph, pathToIndexDir + "" + k + ".rlci");
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                System.out.println("Index cannot be loaded.");
                System.out.println("Start building the RLC index.");
                rlcIndex = new RlcIndex<>(edgeLabeledGraph, k);
                rlcIndex.build();
            }
            for (String pathToWorkload : workloadPaths) {
                List<Query<Integer>> queryList = WorkLoadGenerator.readQueryListFromJsonFile(pathToWorkload);
                final boolean isTrue = queryList.get(0).isTrue();
                // capture workload execution time

                // test
                for (Query<Integer> q : queryList)
                    if (q.isTrue() != rlcIndex.query(q.getSource(), q.getTarget(), q.getLabelConstraint()))
                        System.out.println("wrong results");

                DescriptiveStatistics descriptiveStatistics = new DescriptiveStatistics();
                for (int i = 0; i < 20 + WARM_UP; i++) {
                    long start = System.nanoTime();
                    for (Query<Integer> q : queryList)
                        rlcIndex.query(q.getSource(), q.getTarget(), q.getLabelConstraint());
                    long end = System.nanoTime();
                    if (i < WARM_UP)
                        continue;
                    descriptiveStatistics.addValue(end - start);
                }
                LongArrayList longs = new LongArrayList();
                longs.add((long) descriptiveStatistics.getPercentile(50));
                recordTotalQueryExecution(Triple.of(longs, csvPrinter, new Object[]{graphName, pathToWorkload, k, isTrue}));
            }
        }
    }

    private static void executeBenchmarkWithConfig(Consumer<WorkloadBenchmarkConfig> consumer, String pathToConfig) {
        System.out.println(pathToConfig);
        Gson gson = new Gson();
        Reader reader = null;
        try {
            reader = new FileReader(pathToConfig);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.out.println("Invalid path of benchmark config file.");
        }
        assert reader != null;
        WorkloadBenchmarkConfig workloadBenchmarkConfig = gson.fromJson(reader, WorkloadBenchmarkConfig.class);
        consumer.accept(workloadBenchmarkConfig);
        System.gc();
    }

    private static void executeQueryTimeBenchmarkWithPrebuiltIndex(boolean recordIndividualQueryTime,
                                                                   ExecutionConfig executionConfig,
                                                                   Consumer<Triple<LongList, CSVPrinter, Object[]>> recording) {
        RlcIndex<Integer, RelationshipEdge> rlcIndex = loadRlcIndex(executionConfig);
        if (rlcIndex == null)
            return;
        for (Pair<String, List<Query<Integer>>> pair : executionConfig.getWorkloadNameAndQueryList()) {
            final boolean isTrue = pair.getSecond().get(0).isTrue();
            if (recordIndividualQueryTime) {
                for (int i = 0; i < WARM_UP; i++)
                    for (Query<Integer> q : pair.getSecond()) // warm up
                        rlcIndex.query(q.getSource(), q.getTarget(), q.getLabelConstraint());
                LongArrayList longs = new LongArrayList(); // start experiments
                for (Query<Integer> q : pair.getSecond()) {
                    DescriptiveStatistics descriptiveStatistics = new DescriptiveStatistics();
                    for (int i = 0; i < executionConfig.getWorkloadBenchmarkConfig().getRepeatOfExecution(); i++) { // repeat every query
                        long start = System.nanoTime();
                        rlcIndex.query(q.getSource(), q.getTarget(), q.getLabelConstraint());
                        long end = System.nanoTime();
                        descriptiveStatistics.addValue(end - start); // record the median of repeated executions
                    }
                    longs.add((long) descriptiveStatistics.getPercentile(50));
                }
                recording.accept(Triple.of(longs, executionConfig.getCsvPrinter(), new Object[]{executionConfig.getGraphName(), pair.getFirst(), CSV_COLUMN_NAME_RLC, isTrue}));
            } else { // capture workload execution time

                // For test only
                for (Query<Integer> q : pair.getSecond()) {
                    if (rlcIndex.query(q.getSource(), q.getTarget(), q.getLabelConstraint()) != q.isTrue()) {
                        System.out.println("Wrong results!!!");
                        System.out.println(q);
                    }
                }

                DescriptiveStatistics descriptiveStatistics = new DescriptiveStatistics();
                LongArrayList longs = new LongArrayList();
                for (int i = 0; i < executionConfig.getWorkloadBenchmarkConfig().getRepeatOfExecution() + WARM_UP; i++) {
                    long start = System.nanoTime();
                    for (Query<Integer> q : pair.getSecond())
                        rlcIndex.query(q.getSource(), q.getTarget(), q.getLabelConstraint());
                    long end = System.nanoTime();
                    if (i < WARM_UP)
                        continue;
                    descriptiveStatistics.addValue(end - start);
                }
                longs.add((long) descriptiveStatistics.getPercentile(50));
                recording.accept(Triple.of(longs, executionConfig.getCsvPrinter(), new Object[]{executionConfig.getGraphName(), pair.getFirst(), CSV_COLUMN_NAME_RLC, isTrue}));
                System.gc();
            }
        }
    }

    private static void openBenchmark(final WorkloadBenchmarkConfig workloadBenchmarkConfig, final Consumer<ExecutionConfig> benchmarkTask, final String[] header) {
        CSVPrinter csvPrinter = getCSVPrinter(workloadBenchmarkConfig.getPathToWrite(), header);
        if (csvPrinter == null)
            return;
        Set<String> skippedGraphs = new HashSet<>(Arrays.asList(workloadBenchmarkConfig.getSkippedGraphNames()));
        List<String> fullWorkloadPaths;
        try {
            fullWorkloadPaths = Files.walk(Paths.get(workloadBenchmarkConfig.getPathToWorkLoadRootDirector()))
                    .filter(Files::isRegularFile)
                    .map(Path::toString)
                    .filter(s -> s.contains(".json"))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            e.printStackTrace();
            System.out.println("Invalid path of workload root director");
            return;
        }
        for (String pathToGraph : EdgeList.listPaths(workloadBenchmarkConfig.getPathToGraphRootDirectory())) {
            final String graphName = Paths.get(pathToGraph).getFileName().toString().replace(".txt", "");
            if (skippedGraphs.contains(graphName))
                continue;
            List<String> workloadPaths = fullWorkloadPaths.stream()
                    .filter(s -> s.contains(graphName)) // get all workloads for the given graphs
                    .collect(Collectors.toList());
            List<Pair<String, List<Query<Integer>>>> workloadNameAndQueryList = new ArrayList<>();
            workloadPaths.forEach(workloadPath -> {
                String workLoadName = workloadPath.replace(workloadBenchmarkConfig.getPathToWorkLoadRootDirector(), "");
                List<Query<Integer>> queryList = readQueryListsFromJsonFiles(workloadPath).get(0);
                workloadNameAndQueryList.add(Pair.of(workLoadName, queryList));
            });
            benchmarkTask.accept(new ExecutionConfig(workloadBenchmarkConfig, csvPrinter, pathToGraph, workloadNameAndQueryList, graphName));
        }
        try {
            csvPrinter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void executeExecutionTimeBenchmark(ExecutionConfig executionConfig, Consumer<Triple<LongList, CSVPrinter, Object[]>> recording) {
        String pathToGraph = executionConfig.pathToGraph;
        String graphName = executionConfig.graphName;
        WorkloadBenchmarkConfig workloadBenchmarkConfig = executionConfig.workloadBenchmarkConfig;
        CSVPrinter csvPrinter = executionConfig.csvPrinter;
        final EdgeLabeledGraph<Integer, RelationshipEdge> graph = EdgeList.read(pathToGraph, graphName);
        if (graph == null)
            return;
        for (String method : workloadBenchmarkConfig.getMethods()) {
            if (method == null)
                continue;
            System.out.println("Current method: " + method);
            // Build ETC
            if (CSV_COLUMN_NAME_ETC.equals(method)) {
                ExtendedTransitiveClosure<Integer, RelationshipEdge> transitiveClosure = new ExtendedTransitiveClosure<>(graph, 2);
                if (runWithTimeOut(24 * 60, transitiveClosure::build)) {  // build transitive closure
                    System.out.println("Time out!!!");
                    continue;
                }
                System.out.println("Size of Etc: " + GraphLayout.parseInstance(transitiveClosure.getTC()).totalSize());
                for (Pair<String, List<Query<Integer>>> pair : executionConfig.workloadNameAndQueryList) {
                    final boolean isTrue = pair.getSecond().get(0).isTrue();
                    long start, end;
                    for (int i = 0; i < workloadBenchmarkConfig.getRepeatOfExecution() + WARM_UP; i++) {
                        LongArrayList queryTimeList = new LongArrayList(pair.getSecond().size());
                        start = System.nanoTime();
                        for (Query<Integer> q : pair.getSecond()) {
                            transitiveClosure.query(q.getSource(), q.getTarget(), q.getLabelConstraint());
                        }
                        end = System.nanoTime();
                        queryTimeList.add(end - start);
                        if (i < WARM_UP)
                            continue;
                        recording.accept(Triple.of(queryTimeList, csvPrinter, new Object[]{graphName, pair.getFirst(), method, isTrue}));
                    }
                }

            } else {
                for (Pair<String, List<Query<Integer>>> pair : executionConfig.workloadNameAndQueryList) {
                    final boolean isTrue = pair.getSecond().get(0).isTrue();
                    long start, end;
                    switch (method) {
                        case CSV_COLUMN_NAME_BFS:
                            for (int i = 0; i < workloadBenchmarkConfig.getRepeatOfExecution() + WARM_UP; i++) {
                                LongArrayList queryTimeList = new LongArrayList(pair.getSecond().size());
                                boolean timeOut = false;
                                start = System.nanoTime();
                                for (Query<Integer> q : pair.getSecond()) {
                                    OnlineTraversal.reachBfs(graph, q.getSource(), q.getTarget(), q.getLabelConstraint());
                                    if ((System.nanoTime() - start) / 60_000_000_000L > workloadBenchmarkConfig.getTimeOutDurationInMinutes()) {
                                        timeOut = true;
                                        break;
                                    }
                                }
                                end = System.nanoTime();
                                if (timeOut) {
                                    System.out.println("Time out!!! Method: " + method + ", graph: " + graphName + ", workload: " + pair.getFirst());
                                    break;
                                } else {
                                    queryTimeList.add(end - start);
                                    if (i < WARM_UP)
                                        continue;
                                    recording.accept(Triple.of(queryTimeList, csvPrinter, new Object[]{graphName, pair.getFirst(), method, isTrue}));
                                }
                            }
                            break;
                        case CSV_COLUMN_NAME_BIBFS:
                            for (int i = 0; i < workloadBenchmarkConfig.getRepeatOfExecution() + WARM_UP; i++) {
                                LongArrayList queryTimeList = new LongArrayList(pair.getSecond().size());
                                boolean timeOut = false;
                                start = System.nanoTime();
                                for (Query<Integer> q : pair.getSecond()) {
                                    OnlineTraversal.reachBiBfs(graph, q.getSource(), q.getTarget(), q.getLabelConstraint());
                                    if ((System.nanoTime() - start) / 60_000_000_000L > workloadBenchmarkConfig.getTimeOutDurationInMinutes()) {
                                        timeOut = true;
                                        break;
                                    }
                                }
                                end = System.nanoTime();
                                if (timeOut) {
                                    System.out.println("Time out!!! Method: " + method + ", graph: " + graphName + ", workload: " + pair.getFirst());
                                    break;
                                } else {
                                    queryTimeList.add(end - start);
                                    if (i < WARM_UP)
                                        continue;
                                    recording.accept(Triple.of(queryTimeList, csvPrinter, new Object[]{graphName, pair.getFirst(), method, isTrue}));
                                }
                            }
                            break;
                        case CSV_COLUMN_NAME_RLC:
                            RlcIndex<Integer, RelationshipEdge> klIndex = new RlcIndex<>(graph, 2);
                            if (runWithTimeOut(24 * 60, klIndex::build)) // build index
                                System.out.println("Time out!!!");
                            else
                                for (int i = 0; i < workloadBenchmarkConfig.getRepeatOfExecution() + WARM_UP; i++) {
                                    LongArrayList queryTimeList = new LongArrayList(pair.getSecond().size());
                                    start = System.nanoTime();
                                    for (Query<Integer> q : pair.getSecond()) {
                                        klIndex.query(q.getSource(), q.getTarget(), q.getLabelConstraint());
                                    }
                                    end = System.nanoTime();
                                    queryTimeList.add(end - start);
                                    if (i < WARM_UP)
                                        continue;
                                    recording.accept(Triple.of(queryTimeList, csvPrinter, new Object[]{graphName, pair.getFirst(), method, isTrue}));
                                }
                            break;
                        default:
                            System.out.println("Invalid method!!!");
                            break;
                    }
                }
            }
        }
    }

    public static CSVPrinter getCSVPrinter(String pathToWrite, String[] header) {
        Path pathToResults;
        String resultFile;
        try {
            pathToResults = Files.createDirectories(Paths.get(pathToWrite));
            resultFile = Files.createFile(Paths.get(pathToResults.toString() + File.separator + java.time.LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + "-" + java.time.LocalDateTime.now().getHour() + "h" + java.time.LocalDateTime.now().getMinute() + "m" + ".csv")).toString();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Cannot create the result directory.");
            return null;
        }
        CSVPrinter csvPrinter;
        try {
            csvPrinter = new CSVPrinter(new FileWriter(resultFile), CSVFormat.Builder.create().setHeader(header).build());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Invalid result path and CSVPrinter cannot be initialized.");
            csvPrinter = null;
        }
        return csvPrinter;
    }

    private static RlcIndex<Integer, RelationshipEdge> loadRlcIndex(ExecutionConfig executionConfig) {
        EdgeLabeledGraph<Integer, RelationshipEdge> graph = EdgeList.read(executionConfig.getPathToGraph(), executionConfig.getGraphName());
        String graphName = executionConfig.getGraphName();
        String graphType;
        if (Character.isDigit(graphName.charAt(0)) && Character.isDigit(graphName.charAt(1))) {
            graphType = "real-world-graphs/k-2/";
        } else if (graphName.contains("er-"))
            graphType = "synthetic-graphs/er-zipf/k-2/";
        else if (graphName.contains("ba-"))
            graphType = "synthetic-graphs/ba-zipf/k-2/";
        else
            return null;
        System.out.println("graph name: " + executionConfig.getGraphName());
        System.out.println("graph model: " + graphType);
        RlcIndex<Integer, RelationshipEdge> rlcIndex = null;
        try {
            rlcIndex = RlcIndex.deserialize(graph, PATH_TO_PREBUILT_RLC_INDEX_DIR + graphType + executionConfig.getGraphName() + ".rlci"); // get the index for the given graph
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            System.out.println("Index cannot be loaded.");
            System.out.println("Start building the RLC index");
            rlcIndex = new RlcIndex<>(graph, 2);
            rlcIndex.build();
        }
        return rlcIndex;
    }

    public static void recordIndividualQueryExecution(Triple<LongList, CSVPrinter, Object[]> triple) {
        LongList listOfIndividualQueryTime = triple.getFirst();
        CSVPrinter csvPrinter = triple.getSecond();
        Object[] objects = triple.getThird();
        int i = 1;
        for (long queryTime : listOfIndividualQueryTime) {
            ArrayList<Object> arrayList = new ArrayList<>(Arrays.asList(objects));
            arrayList.add(i++);
            arrayList.add(queryTime);
            record(csvPrinter, arrayList);
        }
    }

    public static void recordTotalQueryExecution(Triple<LongList, CSVPrinter, Object[]> triple) {
        CSVPrinter csvPrinter = triple.getSecond();
        ArrayList<Object> arrayList = new ArrayList<>(Arrays.asList(triple.getThird()));
        arrayList.add(triple.getFirst().longStream().sum());
        record(csvPrinter, arrayList);
    }

    private static void record(CSVPrinter csvPrinter, List<?> values) {
        try {
            csvPrinter.printRecord(values);
            csvPrinter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class ExecutionConfig {
        WorkloadBenchmarkConfig workloadBenchmarkConfig;
        CSVPrinter csvPrinter;
        String pathToGraph;
        List<Pair<String, List<Query<Integer>>>> workloadNameAndQueryList;
        String graphName;

        public ExecutionConfig(WorkloadBenchmarkConfig workloadBenchmarkConfig, CSVPrinter csvPrinter, String pathToGraph, List<Pair<String, List<Query<Integer>>>> workloadNameAndQueryList, String graphName) {
            this.workloadBenchmarkConfig = workloadBenchmarkConfig;
            this.csvPrinter = csvPrinter;
            this.pathToGraph = pathToGraph;
            this.workloadNameAndQueryList = workloadNameAndQueryList;
            this.graphName = graphName;
        }

        public WorkloadBenchmarkConfig getWorkloadBenchmarkConfig() {
            return workloadBenchmarkConfig;
        }

        public CSVPrinter getCsvPrinter() {
            return csvPrinter;
        }

        public String getPathToGraph() {
            return pathToGraph;
        }

        public List<Pair<String, List<Query<Integer>>>> getWorkloadNameAndQueryList() {
            return workloadNameAndQueryList;
        }

        public String getGraphName() {
            return graphName;
        }
    }
}

