package org.jrpq.rlci.benchmark.util.config;

public class WorkloadBenchmarkConfig extends BenchmarkConfig {
    private final String pathToWorkLoadRootDirector;

    public WorkloadBenchmarkConfig(String pathToGraphRootDirectory, String pathToWorkLoadRootDirector, String pathToResults, String[] methods, int repeatOfExecution, long timeOutDurationInMinutes, String[] skippedGraphNames) {
        super(pathToGraphRootDirectory, pathToResults, methods, repeatOfExecution, timeOutDurationInMinutes, skippedGraphNames);
        this.pathToWorkLoadRootDirector = pathToWorkLoadRootDirector;
    }

    public String getPathToWorkLoadRootDirector() {
        return pathToWorkLoadRootDirector;
    }

    @Override
    public String toString() {
        return "WorkloadBenchmarkConfig{" +
                "pathToWorkLoadRootDirector='" + pathToWorkLoadRootDirector + '\'' +
                '}';
    }
}
