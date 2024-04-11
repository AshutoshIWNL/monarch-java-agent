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

    public String getTraceFileLocation() {
        return traceFileLocation;
    }

    public void setTraceLocation(String traceLocation) {
        this.traceFileLocation = traceLocation;
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

    @Override
    public String toString() {
        return "Config{" +
                "traceLocation='" + traceFileLocation + '\'' +
                ", agentFilters=" + agentFilters +
                ", printClassLoaderTrace=" + printClassLoaderTrace +
                ", jvmHeapUsage='" + printJVMHeapUsage + '\'' +
                ", printJVMSystemProperties=" + printJVMSystemProperties +
                '}';
    }
}
