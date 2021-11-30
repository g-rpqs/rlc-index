package org.jrpq.rlci.benchmark.util.generators;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.jrpq.rlci.benchmark.util.EdgeList;
import org.jrpq.rlci.benchmark.util.config.WorkLoadGeneratorConfig;
import org.jrpq.rlci.core.graphs.EdgeLabeledGraph;
import org.jrpq.rlci.core.graphs.RelationshipEdge;
import org.jrpq.rlci.core.kbs.MinimumRepeat;
import it.unimi.dsi.fastutil.Pair;
import org.jgrapht.alg.util.Triple;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;

import static org.jrpq.rlci.benchmark.util.EdgeList.listPaths;
import static org.jrpq.rlci.benchmark.util.generators.QueryGenerator.*;

public final class WorkLoadGenerator {

    public static <V, E> void queryListGenerator(EdgeLabeledGraph<V, E> graph, int k, List<Query<V>> positiveQueries, List<Query<V>> negativeQueries, int numQueries, BiFunction<EdgeLabeledGraph<V, E>, Triple<V, V, String[]>, Pair<Boolean, Integer>> genF, IntSupplier lengthSupplier) {
        int numOfVertices = graph.getNumberOfVertices();
        Random random = new Random();
        String[] labelSet = graph.getLabelSet().toArray(new String[0]);
        int numIte = 10;
        int d = 2 * k;
        while (positiveQueries.size() < numQueries || negativeQueries.size() < numQueries) {
            V source = graph.getVertex(random.nextInt(numOfVertices));
            for (int i = 0; i < numIte; i++) {
                V target = graph.getVertex(random.nextInt(numOfVertices));
                Collection<String[]> constraints = labelConstraintsGenerate(graph, labelSet, lengthSupplier);
                for (String[] cons : constraints) {
                    Pair<Boolean, Integer> resultOfSearch = genF.apply(graph, Triple.of(source, target, cons));
                    if (resultOfSearch.right() >= d) {
                        Query<V> query = new Query<>(source, target, cons, resultOfSearch.left(), resultOfSearch.right());
                        boolean isAdded = false;
                        if (resultOfSearch.left() && positiveQueries.size() < numQueries) {
                            positiveQueries.add(query);
                            isAdded = true;
                        }
                        if (!resultOfSearch.left() && negativeQueries.size() < numQueries) {
                            negativeQueries.add(query);
                            isAdded = true;
                        }
                        if (isAdded) {
                            System.out.println("Graph: " + graph.getGraphName() + ", size of positive-queries: " + positiveQueries.size() + ", size of negative-queries: " + negativeQueries.size());
                        }
                        if (positiveQueries.size() == numQueries && negativeQueries.size() == numQueries)
                            return;
                    }
                }
            }
        }
    }

    public static <V, E> Collection<String[]> labelConstraintsGenerate(EdgeLabeledGraph<V, E> graph, String[] labelSet, IntSupplier lengthSupplier) {
        Collection<String[]> ret = new HashSet<>();
        Random random = new Random();
        int numOfLabels = labelSet.length;
        while (ret.size() < labelSet.length) {
            int length = lengthSupplier.getAsInt();
            String[] cons = new String[length];
            for (int j = 0; j < length; j++)
                cons[j] = labelSet[random.nextInt(numOfLabels)]; // random selection of edge labels in labelSet
            int[] encodedCons = new int[length];
            for (int i = 0; i < length; i++) // initialize encoded constraints
                encodedCons[i] = graph.encodeEdgeLabel(cons[i]);
            if (MinimumRepeat.computeMinimumRepeat(encodedCons).length() == encodedCons.length) {// filter out a constraint, such that its length is larger than the minimum repeat of the constraint, e.g., (a,a)
                ret.add(cons);
            }
        }
        return ret;
    }

    public static void generateQueryListsAndWriteIntoJson(String pathToGraph, String pathToWritePositives, String pathToWriteNegatives, int numOfQueries, IntSupplier lengthSupplier) {
        EdgeLabeledGraph<Integer, RelationshipEdge> graph = EdgeList.read(pathToGraph, pathToGraph);
        List<Query<Integer>> positives = new ArrayList<>(), negatives = new ArrayList<>();
        BiFunction<EdgeLabeledGraph<Integer, RelationshipEdge>, Triple<Integer, Integer, String[]>, Pair<Boolean, Integer>> genF = (g, triple) -> generateQueriesUsingBiBfS(g, triple.getFirst(), triple.getSecond(), triple.getThird());
        queryListGenerator(graph, 2, positives, negatives, numOfQueries, genF, lengthSupplier);
        if (positives.size() == numOfQueries)
            writeIntoJson(pathToWritePositives, positives);
        if (negatives.size() == numOfQueries)
            writeIntoJson(pathToWriteNegatives, negatives);
    }

    public static void writeIntoJson(String path, List<Query<Integer>> queries) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            FileWriter fileWriter = new FileWriter(path);
            gson.toJson(queries, fileWriter);
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeIntoJson2(String path, List<ExtendedQuery<Integer>> queries) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            FileWriter fileWriter = new FileWriter(path);
            gson.toJson(queries, fileWriter);
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<List<Query<Integer>>> readQueryListsFromJsonFiles(String... pathToFiles) {
        List<List<Query<Integer>>> ret = new ArrayList<>(pathToFiles.length);
        for (String path : pathToFiles) {
            Type querySetType = new TypeToken<List<Query<Integer>>>() {
            }.getType();
            Gson gson = new Gson();
            try {
                Reader reader = new FileReader(path);
                List<Query<Integer>> temp = gson.fromJson(reader, querySetType);
                ret.add(temp);
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    public static List<List<Query<Integer>>> readQueryListsFromJsonFiles(String pathToWorkLoadDir) {
        List<String> list = null;
        try {
            list = Files
                    .walk(Paths.get(pathToWorkLoadDir))
                    .filter(Files::isRegularFile)
                    .map(Path::toString)
                    .filter(s -> s.contains(".json"))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert list != null;
        return readQueryListsFromJsonFiles(list.toArray(new String[0]));
    }

    public static List<Query<Integer>> readQueryListFromJsonFile(String pathToFile) {
        Type querySetType = new TypeToken<List<Query<Integer>>>() {
        }.getType();
        Gson gson = new Gson();
        List<Query<Integer>> workloads = null;
        try {
            Reader reader = new FileReader(pathToFile);
            workloads = gson.fromJson(reader, querySetType);
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return workloads;
    }

    public static List<ExtendedQuery<Integer>> readQueryListFromJsonFile2(String pathToFile) {
        Type querySetType = new TypeToken<List<ExtendedQuery<Integer>>>() {
        }.getType();
        Gson gson = new Gson();
        List<ExtendedQuery<Integer>> workloads = null;
        try {
            Reader reader = new FileReader(pathToFile);
            workloads = gson.fromJson(reader, querySetType);
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return workloads;
    }

    public static void generate(String pathToWorkloadGenConfig) {
        Gson gson = new Gson();
        Reader reader = null;
        try {
            reader = new FileReader(pathToWorkloadGenConfig);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.out.println("Invalid path to read the configuration file");
        }
        assert reader != null;
        WorkLoadGeneratorConfig workLoadGeneratorConfig = gson.fromJson(reader, WorkLoadGeneratorConfig.class);
        Set<String> skippedGraphs = new HashSet<>(Arrays.asList(workLoadGeneratorConfig.getSkippedGraphNames()));
        List<String> pathsToDatasets = listPaths(workLoadGeneratorConfig.getPathToGraphRootDirectory());

        String[] queryTypes = new String[]{
                "rlc-queries-len-1",
                "rlc-queries-len-2"
        };

        for (String queryType : queryTypes) {
            IntSupplier lengthSupplier;
            if (queryType.contains("len2")) {
                lengthSupplier = () -> 2;
            } else {
                lengthSupplier = () -> 1;
            }
            for (String path : pathsToDatasets) {
                final String graphName = Paths.get(path).getFileName().toString().replace(".txt", "");
                if (skippedGraphs.contains(graphName))
                    continue;
                System.out.println("Generating workloads for graph: " + graphName);
                for (String generationMethod : workLoadGeneratorConfig.getGenerationMethods()) {
                    final String p = workLoadGeneratorConfig.getPathToWrite() + File.separator + queryType + File.separator;
                    try {
                        Files.createDirectories(Paths.get(p));
                    } catch (Exception e) {
                        e.printStackTrace();
                        continue;
                    }
                    System.out.println("The task of generating workloads for " + graphName + " starts.");
                    System.out.println("Generation method: " + generationMethod);
                    generateQueryListsAndWriteIntoJson(path, p + graphName + "-positive.json", p + graphName + "-negative.json", 1_000, lengthSupplier);
                    System.out.println("The task of generating workloads for " + graphName + " is finished.");
                }
            }
        }
    }
}
