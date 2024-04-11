package com.asm.mja;

import com.asm.mja.config.Config;
import com.asm.mja.config.ConfigParser;
import com.asm.mja.config.ConfigValidator;
import com.asm.mja.logging.AgentLogger;
import com.asm.mja.logging.LogLevel;
import com.asm.mja.transformer.GlobalTransformer;
import com.asm.mja.utils.BannerUtils;

import java.io.File;
import java.lang.instrument.Instrumentation;

/**
 * @author ashut
 * @since 11-04-2024
 */

public class Agent {

    private final static String AGENT_NAME = "Monarch";
    private final static String VERSION = "1.0";
    private final static String DEFAULT_LOG_LEVEL = "INFO";
    private final static String DEFAULT_AGENT_LOG_DIR = System.getProperty("java.io.tmpdir");

    public static void premain(String agentArgs, Instrumentation inst) {
        setupLogger(agentArgs);
        instrument(agentArgs, inst, "javaagent");
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        setupLogger(agentArgs);
        instrument(agentArgs, inst, "attachVM");
    }

    private static void instrument(String agentArgs, Instrumentation inst, String launchType) {
        printStartup(agentArgs);

        String configFile = null;
        try {
            configFile = fetchConfigFile(agentArgs);;
        } catch (RuntimeException re) {
            AgentLogger.error("Exiting" + AGENT_NAME + " Java Agent due to exception - " + re.getMessage());
            return;
        }

        Config config = null;
        try {
            config = ConfigParser.parse(configFile);
        } catch (RuntimeException re) {
            AgentLogger.error("Exiting" + AGENT_NAME + " Java Agent due to exception - " + re.getMessage());
            return;
        }

        if(!ConfigValidator.isValid(config)) {
            AgentLogger.error("Config file isn't valid, exiting...");
            return;
        }

        GlobalTransformer globalTransformer = new GlobalTransformer(configFile, config);
        inst.addTransformer(globalTransformer);
        AgentLogger.info("Registered transformer - " + GlobalTransformer.class);
        AgentLogger.deinit();
    }

    private static void setupLogger(String agentArgs) {
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
        AgentLogger.debug("AgentLogger initialized");
    }

    private static String fetchConfigFile(String agentArgs) {
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

    public static boolean isValidLogLevel(String input) {
        for (LogLevel level : LogLevel.values()) {
            if (level.name().equals(input)) {
                return true;
            }
        }
        return false;
    }

    public static void printStartup(String agentArgs) {
        AgentLogger.draw(BannerUtils.getBanner(AGENT_NAME + " JAVA AGENT"));
        AgentLogger.info("Starting " + AGENT_NAME + " " + VERSION);
        AgentLogger.info("Agent arguments - " + agentArgs);
    }
}
