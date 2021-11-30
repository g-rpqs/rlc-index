package org.jrpq.rlci.benchmark.util;

import org.jrpq.rlci.core.graphs.JgtEdgeLabeledGraph;
import org.jrpq.rlci.core.graphs.RelationshipEdge;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.apache.commons.math3.distribution.ZipfDistribution;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.alg.util.Triple;
import org.jgrapht.graph.DirectedPseudograph;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class EdgeList {
    private final List<Pair<Integer, Integer>> edges;
    private final Int2IntOpenHashMap edgeToEncodedLabel;
    private final Object2IntOpenHashMap<String> stringEdgeLabelEncoder;
    private final int numOfVertices;

    private EdgeList(List<Pair<Integer, Integer>> edges, Int2IntOpenHashMap edgeToEncodedLabel, Object2IntOpenHashMap<String> stringEdgeLabelEncoder, int numOfVertices) {
        this.edges = edges;
        this.edgeToEncodedLabel = edgeToEncodedLabel;
        this.stringEdgeLabelEncoder = stringEdgeLabelEncoder;
        this.numOfVertices = numOfVertices;
    }

    private static int computeId(Object2IntOpenHashMap<String> map, String s) {
        if (map.containsKey(s))
            return map.getInt(s);
        else {
            int id = map.size();
            map.put(s, id);
            return id;
        }
    }

    public static JgtEdgeLabeledGraph read(String pathToGraph, String graphName, String delimiter, Triple<Integer, Integer, Integer> sourceLabelTarget, String skippedLineStart, Supplier<Integer> edgeLabelSupplier) {
        EdgeList edgeList = readGeneralFiles(pathToGraph, delimiter, sourceLabelTarget, skippedLineStart, edgeLabelSupplier);
        if (edgeList == null)
            return null;
        DirectedPseudograph<Integer, RelationshipEdge> graph = new DirectedPseudograph<>(RelationshipEdge.class);
        for (int i = 0; i < edgeList.numOfVertices; i++)
            graph.addVertex(i);
        int i = 0;
        for (Pair<Integer, Integer> edge : edgeList.edges)
            graph.addEdge(edge.getFirst(), edge.getSecond(), new RelationshipEdge(edgeList.edgeToEncodedLabel.get(i++)));
        System.out.println(graphName + " has been loaded");
        System.out.println("num of vertices: " + graph.vertexSet().size());
        System.out.println("num of edges: " + graph.edgeSet().size());
        System.out.println("num of edge-labels: " + edgeList.getStringEdgeLabelEncoder().size());
        return new JgtEdgeLabeledGraph(graph, edgeList.stringEdgeLabelEncoder, graphName);
    }

    public static EdgeList readGeneralFiles(String pathToGraph, String delimiter, Triple<Integer, Integer, Integer> sourceLabelTarget, String skippedLineStart, Supplier<Integer> edgeLabelSupplier) {
        int sourceIndex = sourceLabelTarget.getFirst(), targetIndex = sourceLabelTarget.getThird();
        Integer labelIndex = sourceLabelTarget.getSecond();
        List<Pair<Integer, Integer>> edges = new ArrayList<>();
        Int2IntOpenHashMap edgeToEncodedLabel = new Int2IntOpenHashMap();
        Object2IntOpenHashMap<String> edgeLabelEncoder = new Object2IntOpenHashMap<>();
        Object2IntOpenHashMap<String> mapToVertexId = new Object2IntOpenHashMap<>(); // make the vId start from 0
        try {
            Scanner scanner = new Scanner(new File(pathToGraph));
            String line;
            String[] data;
            while (scanner.hasNext()) {
                line = scanner.nextLine();
                if (skippedLineStart != null && line.startsWith(skippedLineStart))
                    continue;
                data = line.split(delimiter);
                int source, target;
                try {
                    source = computeId(mapToVertexId, data[sourceIndex]);
                    target = computeId(mapToVertexId, data[targetIndex]);
                } catch (NumberFormatException numberFormatException) {
                    System.out.println(line);
                    continue;
                }
                int edgeLabel = labelIndex != null ? computeId(edgeLabelEncoder, data[labelIndex]) : computeId(edgeLabelEncoder, String.valueOf(edgeLabelSupplier.get()));
                edgeToEncodedLabel.put(edges.size(), edgeLabel);
                edges.add(Pair.of(source, target));
            }
            return new EdgeList(edges, edgeToEncodedLabel, edgeLabelEncoder, mapToVertexId.size());
        } catch (FileNotFoundException e) {
            System.out.println("Invalid path: " + pathToGraph);
            e.printStackTrace();
            return null;
        }
    }

    public static JgtEdgeLabeledGraph read(String readPath, String graphName) {
        return read(readPath, graphName, "\t", Triple.of(0, 1, 2), "#", null);
    }

    public static void readAndWrite(String readPath, String delimiter, Triple<Integer, Integer, Integer> sourceLabelTarget, String skippedLineStart, String writePath, Supplier<Integer> edgeLabelSupplier) {
        int sourceIndex = sourceLabelTarget.getFirst(), targetIndex = sourceLabelTarget.getThird();
        Integer labelIndex = sourceLabelTarget.getSecond();
        Object2IntOpenHashMap<String> edgeLabelEncoder = new Object2IntOpenHashMap<>();
        try {
            Scanner scanner = new Scanner(new File(readPath));
            FileWriter fileWriter = new FileWriter(writePath);
            String line;
            String[] data;
            while (scanner.hasNext()) {
                line = scanner.nextLine();
                if (skippedLineStart != null && line.startsWith(skippedLineStart)) {
                    fileWriter.append(line.replace(skippedLineStart, "#")).append("\n").flush();
                    continue;
                }
                data = line.split(delimiter);
                int edgeLabel = labelIndex != null ? computeId(edgeLabelEncoder, data[labelIndex]) : computeId(edgeLabelEncoder, String.valueOf(edgeLabelSupplier.get()));
                fileWriter.append(data[sourceIndex]).append("\t").append(String.valueOf(edgeLabel)).append("\t").append(data[targetIndex]).append("\n").flush(); // vertices are not changed
            }
            fileWriter.close();
        } catch (FileNotFoundException e) {
            System.out.println("Invalid path: " + readPath);
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void readAndWriteWithZipfEdgeLabels(String readPath, String delimiter, int sourceIndex, int targetIndex, String skippedLineStart, String writePath, int numOfLabels) {
        ZipfDistribution zipfDistribution = new ZipfDistribution(numOfLabels, 2);
        readAndWrite(readPath, delimiter, Triple.of(sourceIndex, null, targetIndex), skippedLineStart, writePath, zipfDistribution::sample);
    }

    public static void addZipfEdgeLabelsToEdgeListGraphs(String readPathToEdgeList, String writeDirector, Predicate<String> skippedPaths, String delimiter, int... labelSizes) {
        for (int size : labelSizes) {
            for (String path : listPaths(readPathToEdgeList)) {
                if (skippedPaths != null && skippedPaths.test(path))
                    continue;
                ZipfDistribution zipfDistribution = new ZipfDistribution(size, 2);
                String newGraphName = Paths.get(path).getFileName().toString().replace(".txt", "");
                String p = writeDirector + File.separator + newGraphName + "-" + size + ".txt";
                System.out.println(p);
                readAndWrite(path, delimiter, Triple.of(0, null, 1), "#", p, zipfDistribution::sample);
            }
        }
    }

    public static List<String> listPaths(String root) {
        List<String> list = new ArrayList<>();
        try {
            Files.walk(Paths.get(root))
                    .forEach(subPath -> {
                        if (Files.isRegularFile(subPath))
                            list.add(subPath.toString());
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }

        Collections.sort(list);
        return list;
    }

    public static void addLabelsToStackOverFlowGraph(Path path) {
        if (!Files.isDirectory(path)) {
            System.out.println("Root director of StackOverflow is needed, instead of a path to a file");
        } else {
            String[] fileNames = new String[]{"sx-stackoverflow-a2q.txt", "sx-stackoverflow-c2a.txt", "sx-stackoverflow-c2q.txt"};
            String[] labels = new String[]{"a2q", "c2a", "c2q"};
            FileWriter fileWriter = null;
            try {
                fileWriter = new FileWriter(path + File.separator + "sx-stackoverflow.txt");
            } catch (IOException e) {
                e.printStackTrace();
            }
            assert fileWriter != null;
            for (int i = 0; i < 3; i++) {
                Scanner scanner;
                try {
                    scanner = new Scanner(new File(path + File.separator + fileNames[i]));
                    System.out.println("Start adding labels for " + fileNames[i]);
                    String line;
                    String[] data;
                    while (scanner.hasNext()) {
                        line = scanner.nextLine();
                        data = line.split(" ");
                        fileWriter.append(data[0]).append("\t").append(labels[i]).append("\t").append(data[1]).append("\n").flush();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void addZipfEdgeLabelsToEdgeListGraphs(String readPath, String writePath, String delimiter, int... labelSizes) {
        addZipfEdgeLabelsToEdgeListGraphs(readPath, writePath, null, delimiter, labelSizes);
    }

    public List<Pair<Integer, Integer>> getEdges() {
        return edges;
    }

    public Int2IntOpenHashMap getEdgeToEncodedLabel() {
        return edgeToEncodedLabel;
    }

    public Object2IntOpenHashMap<String> getStringEdgeLabelEncoder() {
        return stringEdgeLabelEncoder;
    }

    public int getNumOfVertices() {
        return numOfVertices;
    }

}
