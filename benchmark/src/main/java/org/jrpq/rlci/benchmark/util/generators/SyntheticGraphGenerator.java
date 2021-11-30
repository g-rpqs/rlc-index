package org.jrpq.rlci.benchmark.util.generators;

import org.jgrapht.generate.BarabasiAlbertGraphGenerator;
import org.jgrapht.generate.GnmRandomGraphGenerator;
import org.jgrapht.graph.AbstractGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedPseudograph;
import org.jgrapht.nio.csv.CSVExporter;
import org.jgrapht.nio.csv.CSVFormat;
import org.jgrapht.util.SupplierUtil;

import java.io.FileWriter;
import java.io.IOException;

public class SyntheticGraphGenerator {

    public static void generateERGraphs(int n, int d, String writeDir) {// ER graphs
        GnmRandomGraphGenerator<Integer, DefaultEdge> gnmRandomGraphGenerator = new GnmRandomGraphGenerator<>(n, n * d, System.currentTimeMillis(), true, true);
        DirectedPseudograph<Integer, DefaultEdge> graph = new DirectedPseudograph<>(SupplierUtil.createIntegerSupplier(), SupplierUtil.createDefaultEdgeSupplier(), false);
        gnmRandomGraphGenerator.generateGraph(graph);
        export(graph, writeDir + "er-" + n + "-" + (n * d) + ".txt");
    }

    public static void generateBAGraphs(int n, int m, int m0, String writeDir) {// BA graphs
        BarabasiAlbertGraphGenerator<Integer, DefaultEdge> barabasiAlbertGraphGenerator = new BarabasiAlbertGraphGenerator<>(m0, m, n);
        DirectedPseudograph<Integer, DefaultEdge> graph = new DirectedPseudograph<>(SupplierUtil.createIntegerSupplier(), SupplierUtil.createDefaultEdgeSupplier(), false);
        barabasiAlbertGraphGenerator.generateGraph(graph);
        System.out.println("|V|:" + graph.vertexSet().size() + ", |E|:" + graph.edgeSet().size());
        export(graph, writeDir + "ba-" + n + "-" + (m * n) + ".txt");
    }


    private static void export(AbstractGraph<Integer, DefaultEdge> graph, String path) {
        CSVExporter<Integer, DefaultEdge> csvExporter = new CSVExporter<>(CSVFormat.EDGE_LIST);
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        csvExporter.exportGraph(graph, fileWriter);
    }
}
