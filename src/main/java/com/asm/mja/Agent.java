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
import com.asm.mja.utils.*;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.*;

/**
 * Monarch's Entry Class
 *
 * @author ashut
 * @since 11-04-2024
 */
public class Agent {

    private final static String AGENT_NAME = "Monarch";
    private final static String JAVA_AGENT_MODE = "javaagent";
    private final static String ATTACH_VM_MODE = "attachVM";
    private final static String VERSION = "2.0";

    /**
     * Entry point for premain. Sets up the logger and starts instrumenting.
     *
     * @param agentArgs  The agent arguments passed to the JVM.
     * @param inst       The instrumentation instance.
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        AgentConfigurator.setupLogger(agentArgs);
        instrument(agentArgs, inst, JAVA_AGENT_MODE);
    }

    /**
     * Entry point for agentmain. Sets up the logger and starts instrumenting.
     *
     * @param agentArgs  The agent arguments passed to the JVM.
     * @param inst       The instrumentation instance.
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        AgentConfigurator.setupLogger(agentArgs);
        instrument(agentArgs, inst, ATTACH_VM_MODE);
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
        } catch (IllegalArgumentException re) {
            AgentLogger.error("Exiting" + AGENT_NAME + " Java Agent due to exception - " + re.getMessage(),re);
            return;
        }

        Config config = null;
        try {
            config = ConfigParser.parse(configFile);
        } catch (RuntimeException re) {
            AgentLogger.error(String.format("Exiting %s Java Agent due to exception - %s", AGENT_NAME, re.getMessage()), re);
            return;
        }

        if(!ConfigValidator.isValid(config)) {
            AgentLogger.error("Config file isn't valid, exiting...");
            return;
        }

        if(!config.isShouldInstrument()) {
            AgentLogger.warning("ShouldInstrument is set to false, exiting!");
            return;
        }

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

        List<String> filtersString = new ArrayList<String>(config.getAgentFilters());
        List<Filter> filters = FilterParser.parseFilters(filtersString);
        GlobalTransformer globalTransformer = new GlobalTransformer(config, traceFileLogger, filters);

        if (launchType.equalsIgnoreCase(ATTACH_VM_MODE)) {
            AgentLogger.debug("Launch Type \"" + launchType + "\" detected, going to re-transform classes");
            if (inst.isRetransformClassesSupported()) {
                Class<?>[] classesToInstrument = ClassFilterUtils.filterClasses(inst.getAllLoadedClasses(), filters);
                inst.addTransformer(globalTransformer, Boolean.TRUE);
                try {
                    AgentLogger.debug("Re-transforming classes: " + Arrays.toString(classesToInstrument));
                    inst.retransformClasses(classesToInstrument);
                } catch (UnmodifiableClassException e) {
                    AgentLogger.error("Error re-transforming classes: " + e.getMessage(), e);
                }
            } else {
                AgentLogger.error("Re-transformation not supported by this JVM");
            }
        } else {
            AgentLogger.debug("Launch Type \"" + launchType + "\" detected, going to transform classes");
            inst.addTransformer(globalTransformer, Boolean.FALSE);
        }
        AgentLogger.info("Registered transformer - " + GlobalTransformer.class);

        startInstrumentationManager(inst, configFile, globalTransformer, traceFileLogger, filters, config.getConfigRefreshInterval());

        AgentLogger.debug("Setting up shutdown hook to close resources");
        Thread shutdownHook = new Thread(() -> {
            JVMMemoryMonitor jvmMemoryMonitor = JVMMemoryMonitor.getInstance();
            if(!jvmMemoryMonitor.isDown())
                JVMMemoryMonitor.getInstance().shutdown();
            traceFileLogger.close();
        });
        shutdownHook.setName("monarch-shutdown-hook");
        Runtime.getRuntime().addShutdownHook(shutdownHook);


        AgentLogger.deinit();
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
     * Initializes and starts the Instrumentation Manager with the provided parameters.
     *
     * @param inst                The Instrumentation instance used to perform bytecode manipulation.
     * @param configFile          The path to the configuration file for the Instrumentation Manager.
     * @param globalTransformer   The GlobalTransformer instance that will manage bytecode transformations.
     * @param traceFileLogger     The logger responsible for tracing file operations and instrumentation logs.
     * @param filters             The list of filters to apply during instrumentation.
     * @param configRefreshInterval The interval (in milliseconds) at which the configuration file is checked for updates.
     */
    private static void startInstrumentationManager(Instrumentation inst, String configFile, GlobalTransformer globalTransformer,
                                                   TraceFileLogger traceFileLogger, List<Filter> filters, long configRefreshInterval) {
        InstrumentationManager instrumentationManager = InstrumentationManager.getInstance();
        instrumentationManager.setInstrumentation(inst);
        instrumentationManager.setConfigFilePath(configFile);
        instrumentationManager.setJvmMemoryMonitor(JVMMemoryMonitor.getInstance());
        instrumentationManager.setTransformer(globalTransformer);
        instrumentationManager.setCurrentFilters(filters);
        instrumentationManager.setLastModified(new File(configFile).lastModified());
        instrumentationManager.setLogger(traceFileLogger);
        instrumentationManager.setConfigRefreshInterval(configRefreshInterval);
        instrumentationManager.execute();
    }


    /**
     * Prints the startup information.
     *
     * @param agentArgs The agent arguments passed to the JVM.
     */
    private static void printStartup(String agentArgs) {
        AgentLogger.draw(BannerUtils.getBanner(AGENT_NAME + " JAVA AGENT"));
        AgentLogger.info("Starting " + AGENT_NAME + " " + VERSION + " @ " + DateUtils.getFormattedTimestamp());
        AgentLogger.info("Agent arguments - " + agentArgs);
    }
}
