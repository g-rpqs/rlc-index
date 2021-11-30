package org.jrpq.rlci.benchmark.util.config;

public class WorkLoadGeneratorConfig extends Configuration {
    private final String[] generationMethods;

    public WorkLoadGeneratorConfig(String pathToGraphRootDirectory, String pathToWrite, String[] skippedGraphNames, String[] generationMethods) {
        super(pathToGraphRootDirectory, pathToWrite, skippedGraphNames);
        this.generationMethods = generationMethods;
    }

    public String[] getGenerationMethods() {
        return generationMethods;
    }
}
