package org.jrpq.rlci.benchmark.util.config;

import java.util.Arrays;

public class BenchmarkConfig extends Configuration {
    private final String[] methods;
    private final int repeatOfExecution;
    private final long timeOutDurationInMinutes;

    public BenchmarkConfig(String pathToGraphRootDirectory, String pathToWrite, String[] methods, int repeatOfExecution, long timeOutDurationInMinutes, String[] skippedGraphNames) {
        super(pathToGraphRootDirectory, pathToWrite, skippedGraphNames);
        this.methods = methods;
        this.repeatOfExecution = repeatOfExecution;
        this.timeOutDurationInMinutes = timeOutDurationInMinutes;
    }

    public String[] getMethods() {
        return methods;
    }

    public int getRepeatOfExecution() {
        return repeatOfExecution;
    }

    public long getTimeOutDurationInMinutes() {
        return timeOutDurationInMinutes;
    }

    @Override
    public String toString() {
        return "BenchmarkConfig{" +
                "methods=" + Arrays.toString(methods) +
                ", repeatOfExecution=" + repeatOfExecution +
                ", timeOutDurationInMinutes=" + timeOutDurationInMinutes +
                '}';
    }
}
