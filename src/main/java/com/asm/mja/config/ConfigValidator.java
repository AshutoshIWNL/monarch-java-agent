package com.asm.mja.config;

import com.asm.mja.logging.AgentLogger;

import java.io.File;

/**
 * @author ashut
 * @since 11-04-2024
 */

public class ConfigValidator {
    public static boolean isValid(Config config) {
        AgentLogger.debug("Validating the config object");
        String traceLocation = config.getTraceFileLocation();
        if (traceLocation == null || traceLocation.isEmpty() || !new File(traceLocation).isDirectory()) {
            AgentLogger.error("trace file directory doesn't exist or is not a directory");
            return false;
        }

        if (config.getAgentFilters() == null || config.getAgentFilters().size() == 0) {
            AgentLogger.error("Filters are missing or empty");
            return false;
        }
        return true;
    }
}