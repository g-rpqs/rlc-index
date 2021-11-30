package org.jrpq.rlci.benchmark.util.config;

import java.util.Arrays;

public class Configuration {
    private final String pathToGraphRootDirectory;
    private final String pathToWrite;
    private final String[] skippedGraphNames;

    public Configuration(String pathToGraphRootDirectory, String pathToWrite, String[] skippedGraphNames) {
        this.pathToGraphRootDirectory = pathToGraphRootDirectory;
        this.pathToWrite = pathToWrite;
        this.skippedGraphNames = skippedGraphNames;
    }

    public String getPathToGraphRootDirectory() {
        return pathToGraphRootDirectory;
    }

    public String getPathToWrite() {
        return pathToWrite;
    }

    public String[] getSkippedGraphNames() {
        return skippedGraphNames;
    }

    @Override
    public String toString() {
        return "Configuration{" +
                "pathToGraphRootDirectory='" + pathToGraphRootDirectory + '\'' +
                ", pathToWrite='" + pathToWrite + '\'' +
                ", skippedGraphNames=" + Arrays.toString(skippedGraphNames) +
                '}';
    }
}
