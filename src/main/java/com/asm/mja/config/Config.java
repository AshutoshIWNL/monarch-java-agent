package com.asm.mja.config;

import java.util.HashSet;

/**
 * @author ashut
 * @since 11-04-2024
 */

public class Config {
    private String traceFileLocation;
    private HashSet<String> agentRules;
    private boolean printClassLoaderTrace;
    private boolean printJVMHeapUsage;
    private boolean printJVMSystemProperties;
    private boolean printEnvironmentVariables;

    private int maxHeapDumps;

    private boolean shouldInstrument;

    private int configRefreshInterval;

    public int getConfigRefreshInterval() {
        return configRefreshInterval;
    }

    public void setConfigRefreshInterval(int configRefreshInterval) {
        this.configRefreshInterval = configRefreshInterval;
    }

    public boolean isShouldInstrument() {
        return shouldInstrument;
    }

    public void setShouldInstrument(boolean shouldInstrument) {
        this.shouldInstrument = shouldInstrument;
    }

    public String getTraceFileLocation() {
        return traceFileLocation;
    }

    public void setTraceFileLocation(String traceFileLocation) {
        this.traceFileLocation = traceFileLocation;
    }

    public HashSet<String> getAgentRules() {
        return agentRules;
    }

    public void setAgentRules(HashSet<String> agentRules) {
        this.agentRules = agentRules;
    }

    public boolean isPrintClassLoaderTrace() {
        return printClassLoaderTrace;
    }

    public void setPrintClassLoaderTrace(boolean printClassLoaderTrace) {
        this.printClassLoaderTrace = printClassLoaderTrace;
    }

    public boolean isPrintJVMHeapUsage() {
        return printJVMHeapUsage;
    }

    public void setPrintJVMHeapUsage(boolean printJVMHeapUsage) {
        this.printJVMHeapUsage = printJVMHeapUsage;
    }

    public boolean isPrintJVMSystemProperties() {
        return printJVMSystemProperties;
    }

    public void setPrintJVMSystemProperties(boolean printJVMSystemProperties) {
        this.printJVMSystemProperties = printJVMSystemProperties;
    }

    public boolean isPrintEnvironmentVariables() {
        return printEnvironmentVariables;
    }

    public void setPrintEnvironmentVariables(boolean printEnvironmentVariables) {
        this.printEnvironmentVariables = printEnvironmentVariables;
    }

    public int getMaxHeapDumps() {
        return maxHeapDumps;
    }

    public void setMaxHeapDumps(int maxHeapDumps) {
        this.maxHeapDumps = maxHeapDumps;
    }

    @Override
    public String toString() {
        return "Config{" +
                "traceFileLocation='" + traceFileLocation + '\'' +
                ", agentRules=" + agentRules +
                ", printClassLoaderTrace=" + printClassLoaderTrace +
                ", printJVMHeapUsage=" + printJVMHeapUsage +
                ", printJVMSystemProperties=" + printJVMSystemProperties +
                ", printEnvironmentVariables=" + printEnvironmentVariables +
                ", maxHeapDumps=" + maxHeapDumps +
                '}';
    }
}
