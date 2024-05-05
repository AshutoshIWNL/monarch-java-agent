package com.asm.mja;

import com.asm.mja.config.Config;
import com.asm.mja.config.ConfigParser;
import com.asm.mja.config.ConfigValidator;
import com.asm.mja.filter.Filter;
import com.asm.mja.filter.FilterParser;
import com.asm.mja.logging.AgentLogger;
import com.asm.mja.logging.TraceFileLogger;
import com.asm.mja.monitor.JVMMemoryMonitor;
import com.asm.mja.transformer.GlobalTransformer;
import com.asm.mja.utils.BannerUtils;
import com.asm.mja.utils.DateUtils;
import com.asm.mja.utils.HeapDumpUtils;
import com.asm.mja.utils.JVMUtils;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.*;

/**
 * Monarch's Entry Class
 * @author ashut
 * @since 11-04-2024
 */
public class Agent {

    private final static String AGENT_NAME = "Monarch";
    private final static String VERSION = "1.0";

    /**
     * Entry point for premain. Sets up the logger and starts instrumenting.
     *
     * @param agentArgs  The agent arguments passed to the JVM.
     * @param inst       The instrumentation instance.
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        AgentConfigurator.setupLogger(agentArgs);
        instrument(agentArgs, inst, "javaagent");
    }

    /**
     * Entry point for agentmain. Sets up the logger and starts instrumenting.
     *
     * @param agentArgs  The agent arguments passed to the JVM.
     * @param inst       The instrumentation instance.
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        AgentConfigurator.setupLogger(agentArgs);
        instrument(agentArgs, inst, "attachVM");
    }

    /**
     * Instruments the application with the specified configuration.
     *
     * @param agentArgs  The agent arguments passed to the JVM.
     * @param inst       The instrumentation instance.
     * @param launchType The type of launch: 'javaagent' or 'attachVM'.
     */
    private static void instrument(String agentArgs, Instrumentation inst, String launchType) {
        printStartup(agentArgs);

        String configFile = null;
        try {
            configFile = AgentConfigurator.fetchConfigFile(agentArgs);;
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

        // For limiting the number of heap dumps collected
        HeapDumpUtils.setMaxHeapCount(config.getMaxHeapDumps());

        AgentLogger.debug("Creating TraceFileLogger instance for instrumentation logging");


        TraceFileLogger traceFileLogger = AgentConfigurator.setupTraceFileLogger(config.getTraceFileLocation());

        traceFileLogger.trace(AGENT_NAME + " Java Agent " + VERSION);
        traceFileLogger.trace(JVMUtils.getJVMCommandLine());

        if(config.isPrintJVMSystemProperties()) {
            traceFileLogger.trace(JVMUtils.getJVMSystemProperties());
        }

        if(config.isPrintEnvironmentVariables()) {
            traceFileLogger.trace(JVMUtils.getEnvVars());
        }

        if(config.isPrintJVMHeapUsage()) {
            startJVMMemoryMonitorThread(traceFileLogger);
        }

        ArrayList<String> arrayList = new ArrayList<String>(config.getAgentFilters());
        List<Filter> filters = FilterParser.parseFilters(arrayList);
        GlobalTransformer globalTransformer = new GlobalTransformer(config, traceFileLogger, filters);

        if (launchType.equalsIgnoreCase("attachVM")) {
            AgentLogger.debug("Launch Type \"" + launchType + "\" detected, going to re-transform classes");
            if (inst.isRetransformClassesSupported()) {
                Class<?>[] classesToInstrument = filterClasses(inst.getAllLoadedClasses(), filters);
                inst.addTransformer(globalTransformer, Boolean.TRUE);
                try {
                    AgentLogger.debug("Re-transforming classes: " + Arrays.toString(classesToInstrument));
                    inst.retransformClasses(classesToInstrument);
                } catch (Exception e) {
                    AgentLogger.error("Error re-transforming classes: " + e.getMessage());
                }
            } else {
                AgentLogger.error("Re-transformation not supported by this JVM");
            }
        } else {
            AgentLogger.debug("Launch Type \"" + launchType + "\" detected, going to transform classes");
            inst.addTransformer(globalTransformer, Boolean.FALSE);
        }
        AgentLogger.info("Registered transformer - " + GlobalTransformer.class);

        AgentLogger.debug("Setting up shutdown hook to close resources");
        Thread shutdownHook = new Thread(() -> {
            JVMMemoryMonitor.getInstance().shutdown();
            traceFileLogger.close();
        });
        shutdownHook.setName("monarch-shutdown-hook");
        Runtime.getRuntime().addShutdownHook(shutdownHook);


        AgentLogger.deinit();
    }

    private static Class<?>[] filterClasses(Class<?>[] allLoadedClasses, List<Filter> filters) {
        Set<String> classNamesToInstrument = new HashSet<>();
        for (Filter filter : filters) {
            classNamesToInstrument.add(filter.getClassName());
        }

        List<Class<?>> filteredClasses = new ArrayList<>();
        for (Class<?> clazz : allLoadedClasses) {
            String className = clazz.getName();
            if (classNamesToInstrument.contains(className)) {
                filteredClasses.add(clazz);
            }
        }
        return filteredClasses.toArray(new Class[0]);
    }

    /**
     * Starts the JVM Memory Monitor thread
     *
     * @param traceFileLogger  The logger to be used by JVM Monitor
     */
    private static void startJVMMemoryMonitorThread(TraceFileLogger traceFileLogger) {
        JVMMemoryMonitor jvmMemoryMonitor = JVMMemoryMonitor.getInstance();
        jvmMemoryMonitor.setLogger(traceFileLogger);
        jvmMemoryMonitor.execute();
    }

    /**
     * Prints the startup information.
     *
     * @param agentArgs The agent arguments passed to the JVM.
     */
    public static void printStartup(String agentArgs) {
        AgentLogger.draw(BannerUtils.getBanner(AGENT_NAME + " JAVA AGENT"));
        AgentLogger.info("Starting " + AGENT_NAME + " " + VERSION + " @ " + DateUtils.getFormattedTimestamp());
        AgentLogger.info("Agent arguments - " + agentArgs);
    }
}
