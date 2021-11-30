package org.jrpq.rlci.benchmark.baselines;

import com.google.gson.Gson;
import org.jrpq.rlci.benchmark.util.EdgeList;
import org.jrpq.rlci.benchmark.util.config.WorkloadBenchmarkConfig;
import org.jrpq.rlci.benchmark.util.generators.Query;
import org.jrpq.rlci.core.RlcIndex;
import org.jrpq.rlci.core.graphs.EdgeLabeledGraph;
import org.jrpq.rlci.core.graphs.RelationshipEdge;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.jrpq.rlci.benchmark.runners.IndexingBenchmarkRunner.runWithTimeOut;
import static org.jrpq.rlci.benchmark.util.generators.WorkLoadGenerator.readQueryListsFromJsonFiles;

public class WorkloadBenchmarkTester {

    private static void test(String... pathsToConfig) {
        for (String pathToConfig : pathsToConfig) {
            try {
                Gson gson = new Gson();
                Reader reader = new FileReader(pathToConfig);
                WorkloadBenchmarkConfig workloadBenchmarkConfig = gson.fromJson(reader, WorkloadBenchmarkConfig.class);
                run(workloadBenchmarkConfig);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                System.out.println("Invalid path of benchmark config file.");
            }
        }
    }

    private static void run(final WorkloadBenchmarkConfig workloadBenchmarkConfig) {
        Set<String> skippedGraphs = new HashSet<>(Arrays.asList(workloadBenchmarkConfig.getSkippedGraphNames()));
        for (String method : workloadBenchmarkConfig.getMethods()) {
            for (String pathToGraph : EdgeList.listPaths(workloadBenchmarkConfig.getPathToGraphRootDirectory())) {
                final String graphName = Paths.get(pathToGraph).getFileName().toString().replace(".txt", "");
                if (skippedGraphs.contains(graphName))
                    continue;
                final EdgeLabeledGraph<Integer, RelationshipEdge> graph = EdgeList.read(pathToGraph, graphName);
                Predicate<Query<Integer>> executionMethod = getExecutionMethod(method, graph, workloadBenchmarkConfig.getTimeOutDurationInMinutes());
                if (executionMethod == null)
                    continue;
                System.out.println("Current method: " + method);
                List<String> workloadPaths;
                try {
                    workloadPaths = Files.walk(Paths.get(workloadBenchmarkConfig.getPathToWorkLoadRootDirector()))
                            .filter(Files::isRegularFile)
                            .map(Path::toString)
                            .filter(s -> s.contains(graphName))
                            .collect(Collectors.toList());
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Invalid path of workload root director");
                    continue;
                }
                for (String pathToWorkload : workloadPaths) {
                    System.out.println("Current workload: " + pathToWorkload);
                    for (List<Query<Integer>> workloads : readQueryListsFromJsonFiles(pathToWorkload)) {
                        testQueryResults(workloads, executionMethod);
                    }
                }

            }
        }
    }

    private static void testQueryResults(List<Query<Integer>> list, Predicate<Query<Integer>> predicate) {
        for (Query<Integer> q : list)
            if (q.isTrue() != predicate.test(q)) {
                System.out.println("Wrong results!!!");
                System.out.println(q);
            }
    }

    private static Predicate<Query<Integer>> getExecutionMethod(String method, EdgeLabeledGraph<Integer, RelationshipEdge> graph, long duration) {
        if (method == null)
            return null;
        Predicate<Query<Integer>> executionMethod = null;
        switch (method) {
            case "Bfs": //Bfs
                executionMethod = (q) -> OnlineTraversal.reachBfs(graph, q.getSource(), q.getTarget(), q.getLabelConstraint());
                break;
            case "BiBfs": //BiBfs
                executionMethod = (q) -> OnlineTraversal.reachBiBfs(graph, q.getSource(), q.getTarget(), q.getLabelConstraint());
                break;
            case "RlcIndex": //RlcIndex
                RlcIndex<Integer, RelationshipEdge> rlcIndex = new RlcIndex<>(graph, 2);
                if (runWithTimeOut(duration, rlcIndex::build))
                    System.out.println("Time out!!!");
                else
                    executionMethod = (q) -> rlcIndex.query(q.getSource(), q.getTarget(), q.getLabelConstraint());
                break;
            case "Etc": //Etc
                ExtendedTransitiveClosure<Integer, RelationshipEdge> transitiveClosure = new ExtendedTransitiveClosure<>(graph, 2);
                if (runWithTimeOut(duration, transitiveClosure::build))  // build transitive closure
                    System.out.println("Time out!!!");
                else
                    executionMethod = (q) -> transitiveClosure.query(q.getSource(), q.getTarget(), q.getLabelConstraint());
                break;
            default:
                System.out.println("Invalid method!!!");
                break;
        }
        return executionMethod;
    }
}
