package com.asm.mja;

import com.asm.mja.logging.AgentLogger;
import com.asm.mja.logging.LogLevel;
import com.asm.mja.logging.TraceFileLogger;
import com.asm.mja.utils.DateUtils;
import com.asm.mja.utils.JVMUtils;

import java.io.File;

/**
 * @author ashut
 * @since 23-04-2024
 */

public class AgentConfigurator {
    private final static String DEFAULT_LOG_LEVEL = "INFO";
    private final static String DEFAULT_AGENT_LOG_DIR = System.getProperty("java.io.tmpdir");

    /**
     * Sets up the logger based on the provided agent arguments.
     *
     * @param agentArgs  The agent arguments passed to the JVM.
     */
    public static void setupLogger(String agentArgs) {
        String agentLogFileDir = null;
        try {
            agentLogFileDir = fetchAgentLogFileDir(agentArgs);
            if(agentLogFileDir == null)
                agentLogFileDir = DEFAULT_AGENT_LOG_DIR;
        } catch (IllegalArgumentException e) {
            System.err.println("ERROR: " + e.getMessage());
            agentLogFileDir = DEFAULT_AGENT_LOG_DIR;
        }

        String agentLogFile = agentLogFileDir + File.separator + "monarchAgent.log";

        String agentLogLevel = null;
        try {
            agentLogLevel = fetchAgentLogLevel(agentArgs);
            if(agentLogLevel == null)
                agentLogLevel = DEFAULT_LOG_LEVEL;
        } catch (IllegalArgumentException e) {
            System.err.println("ERROR: " + e.getMessage());
            agentLogLevel = DEFAULT_LOG_LEVEL;
        }
        AgentLogger.init(agentLogFile, LogLevel.valueOf(agentLogLevel));
    }

    /**
     * Fetches the agent log file directory from the agent arguments.
     *
     * @param agentArgs  The agent arguments passed to the JVM.
     * @return The agent log file directory.
     * @throws IllegalArgumentException If the agent log file directory is invalid.
     */
    private static String fetchAgentLogFileDir(String agentArgs) throws IllegalArgumentException {
        String agentLogFileDir = null;
        if (agentArgs != null) {
            String[] args = agentArgs.split(",");
            for (String arg : args) {
                if (arg.contains("agentLogFileDir")) {
                    String[] prop = arg.split("=");
                    if (prop.length < 2) {
                        throw new IllegalArgumentException("Invalid arguments passed - " + arg);
                    } else {
                        agentLogFileDir = prop[1];
                        File agentLogFileDirObj = new File(agentLogFileDir);
                        if (!agentLogFileDirObj.isDirectory() || !agentLogFileDirObj.exists()) {
                            throw new IllegalArgumentException("Agent logging doesn't exist or isn't a directory - " + agentLogFileDir);
                        }
                    }
                }
            }
        }
        return agentLogFileDir;
    }

    /**
     * Fetches the agent log level from the agent arguments.
     *
     * @param agentArgs  The agent arguments passed to the JVM.
     * @return The agent log level.
     * @throws IllegalArgumentException If the agent log level is invalid.
     */
    private static String fetchAgentLogLevel(String agentArgs) throws IllegalArgumentException {
        String agentLogLevel = null;
        if (agentArgs != null) {
            String[] args = agentArgs.split(",");
            for (String arg : args) {
                if (arg.contains("agentLogLevel")) {
                    String[] prop = arg.split("=");
                    if (prop.length < 2) {
                        throw new IllegalArgumentException("Invalid arguments passed - " + arg);
                    } else {
                        agentLogLevel = prop[1];
                        if(!isValidLogLevel(agentLogLevel)) {
                            throw new IllegalArgumentException("Invalid log level passed - " + agentLogLevel);
                        }
                    }
                }
            }
        }
        return agentLogLevel;
    }

    /**
     * Checks if the specified log level is valid.
     *
     * @param input The log level to check.
     * @return True if the log level is valid, otherwise false.
     */
    public static boolean isValidLogLevel(String input) {
        for (LogLevel level : LogLevel.values()) {
            if (level.name().equals(input)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Fetches the configuration file path from the agent arguments.
     *
     * @param agentArgs  The agent arguments passed to the JVM.
     * @return The configuration file path.
     * @throws IllegalArgumentException If the configuration file path is invalid.
     */
    public static String fetchConfigFile(String agentArgs) {
        AgentLogger.debug("Fetching config file to build the agent config");
        String configFile = null;
        if(agentArgs != null) {
            String[] args = agentArgs.split(",");

            for(String arg: args) {
                if(arg.contains("configFile")) {
                    String[] prop = arg.split("=");
                    if(prop.length < 2) {
                        throw new IllegalArgumentException("Invalid arguments passed - " + arg);
                    } else {
                        configFile = prop[1];
                        File configFileObj = new File(configFile);
                        if(!configFileObj.exists()) {
                            throw new IllegalArgumentException("Config file doesn't exist in the specified directory - " + configFile);
                        }
                    }
                }
            }
        }
        return configFile;
    }

    public static TraceFileLogger setupTraceFileLogger(String traceFileLocation) {
        TraceFileLogger traceFileLogger = null;
        String traceDir = traceFileLocation + File.separator + "Monarch_" + JVMUtils.getJVMPID() + "_" + DateUtils.getFormattedTimestampForFileName();
        File traceDirObj = new File(traceDir);
        if(traceDirObj.mkdir()) {
            traceFileLogger = TraceFileLogger.getInstance();
            traceFileLogger.init(traceDirObj.getAbsolutePath());
        }
        else {
            traceFileLogger = TraceFileLogger.getInstance();
            traceFileLogger.init(traceFileLocation);
        }
        return traceFileLogger;
    }
}
