package com.asm.mja;

import com.asm.mja.config.Config;
import com.asm.mja.config.ConfigParser;
import com.asm.mja.filter.Filter;
import com.asm.mja.filter.FilterParser;
import com.asm.mja.logging.TraceFileLogger;
import com.asm.mja.monitor.JVMMemoryMonitor;
import com.asm.mja.transformer.GlobalTransformer;
import com.asm.mja.utils.ByteCodeUtils;
import com.asm.mja.utils.ClassFilterUtils;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author ashut
 * @since 15-08-2024
 */

public class InstrumentationManager implements Runnable {
    private TraceFileLogger logger;
    private Thread thread = null;
    private String configFilePath;

    private long lastModified;

    private Instrumentation instrumentation;

    private List<Filter> currentFilters;

    private JVMMemoryMonitor jvmMemoryMonitor;

    private static InstrumentationManager instance = null;
    private GlobalTransformer transformer;

    private long configRefreshInterval;

    // Cache for original bytecode to avoid redundant file I/O
    private final Map<String, byte[]> bytecodeCache = new HashMap<>();

    public static InstrumentationManager getInstance() {
        if(instance == null) {
            instance = new InstrumentationManager();
        }
        return instance;
    }
    public InstrumentationManager() {

    }

    public void setLogger(TraceFileLogger logger) {
        this.logger = logger;
    }

    public void setConfigFilePath(String configFilePath) {
        this.configFilePath = configFilePath;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public void setInstrumentation(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    public void setTransformer(GlobalTransformer transformer) {
        this.transformer = transformer;
    }

    public void setJvmMemoryMonitor(JVMMemoryMonitor jvmMemoryMonitor) {
        this.jvmMemoryMonitor = jvmMemoryMonitor;
    }

    public void setCurrentFilters(List<Filter> currentFilters) {
        this.currentFilters = currentFilters;
    }

    public void setConfigRefreshInterval(Long configRefreshInterval) {this.configRefreshInterval = configRefreshInterval; }

    @Override
    public void run() {
        while(true) {
            File file = new File(configFilePath);
            Config config = null;
            if(file.lastModified() != lastModified) {
                try {
                    config = ConfigParser.parse(configFilePath, logger);
                    //Handling of change in refresh interval time for config file
                    configRefreshInterval = config.getConfigRefreshInterval();
                } catch (IOException e) {
                    logger.error("Configuration file parsing failed, please verify if it is a valid JSON file after you changes", e);
                    continue;
                }
                if (file.lastModified() != lastModified) {
                    logger.trace("Configuration file has been modified, re-parsing it");
                    handleConfigurationChange(config);
                    lastModified = file.lastModified();
                }
            }
            try {
                Thread.sleep(configRefreshInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupted status
                logger.trace("Instrumentation Manager thread interrupted");
                break;
            }
        }
    }

    private void handleConfigurationChange(Config config) {
        List<Filter> filters = new ArrayList<>(currentFilters);
        resetTransformerState();
        revertInstrumentation(filters);

        if (!config.isShouldInstrument()) {
            logger.trace("Going to shutdown instrumentation");
            shutdown();
            return;
        }

        List<String> filtersString = new ArrayList<>(config.getAgentFilters());
        List<Filter> newFilters = FilterParser.parseFilters(filtersString);
        addNewInstrumentation(newFilters);
        currentFilters = newFilters;
    }

    private void resetTransformerState() {
        transformer.resetClassesTransformed();
        transformer.resetFilters();
    }

    private void addNewInstrumentation(List<Filter> newFilters) {
        transformer.setFilters(newFilters);
        Class<?>[] classesToInstrument = ClassFilterUtils.filterClasses(instrumentation.getAllLoadedClasses(), newFilters);
        for (Class<?> classz : classesToInstrument) {
            String className = classz.getName();
            try {
                /*
                 Using redefine here because re-transform will take the modified byte code as its source and would then result in changes which aren't intended
                 whereas I can pass the source for redefine myself
                 */
                if(bytecodeCache.containsKey(className))
                    instrumentation.redefineClasses(new ClassDefinition(classz, bytecodeCache.get(className)));
                else {
                    //First time for this class: Get the original byte code. Going forward, it will be put in bytecode-cache, so, it will be used from there
                    instrumentation.redefineClasses(new ClassDefinition(classz, ByteCodeUtils.getClassBytecode(classz)));
                }
            } catch (UnmodifiableClassException | ClassNotFoundException | IOException e) {
                logger.error("Failed to re-transform classes;" + "Exception: " + e.getMessage(), e);
            }
        }
    }

    private void revertInstrumentation(List<Filter> currentFilters) {
        if (currentFilters != null) {
            Set<String> classSet = currentFilters.stream().map(Filter::getClassName).collect(Collectors.toSet());
            for (String cName : classSet) {
                loadOriginalByteCode(cName);
            }
        }
        logger.trace("Reverted previous instrumentation");
    }

    private void loadOriginalByteCode(String className) {
        String backupLocation = logger.getTraceDir() + File.separator + "backup" + File.separator + className.substring(className.lastIndexOf(".") + 1) + ".class";
        try {
            byte[] originalBytecode = Files.readAllBytes(Paths.get(backupLocation));
            bytecodeCache.put(className, originalBytecode);
            instrumentation.redefineClasses(new ClassDefinition(Class.forName(className), originalBytecode));
        } catch (IOException | UnmodifiableClassException | ClassNotFoundException e) {
            logger.error("Failed to read bytecode for class " + className + "; Exception: " + e.getMessage(), e);
        }
    }


    public void execute() {
        logger.trace("Starting Monarch Instrumentation Manager");
        thread = new Thread(this, "monarch-inst-manager");
        thread.setDaemon(true);
        thread.start();
    }

    public void shutdown() {
        if(jvmMemoryMonitor != null) {
            jvmMemoryMonitor.shutdown();
        }
        if(thread != null) {
            logger.trace("Shutting down Monarch Instrumentation Manager");
            thread.interrupt();
        }
    }
}
