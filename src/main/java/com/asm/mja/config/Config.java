package com.asm.mja.config;

import java.util.List;

/**
 * @author ashut
 * @since 11-04-2024
 */

public class Config {
    private String traceFileLocation;
    private List<String> agentFilters;
    private boolean printClassLoaderTrace;
    private boolean printJVMHeapUsage;
    private boolean printJVMSystemProperties;
    private boolean printEnvironmentVariables;

    public String getTraceFileLocation() {
        return traceFileLocation;
    }

    public void setTraceFileLocation(String traceFileLocation) {
        this.traceFileLocation = traceFileLocation;
    }

    public List<String> getAgentFilters() {
        return agentFilters;
    }

    public void setAgentFilters(List<String> agentFilters) {
        this.agentFilters = agentFilters;
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

    @Override
    public String toString() {
        return "Config{" +
                "traceFileLocation='" + traceFileLocation + '\'' +
                ", agentFilters=" + agentFilters +
                ", printClassLoaderTrace=" + printClassLoaderTrace +
                ", printJVMHeapUsage=" + printJVMHeapUsage +
                ", printJVMSystemProperties=" + printJVMSystemProperties +
                ", printEnvironmentVariables=" + printEnvironmentVariables +
                '}';
    }
}
