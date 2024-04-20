package com.asm.mja.monitor;

import com.asm.mja.logging.TraceFileLogger;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

/**
 * @author ashut
 * @since 19-04-2024
 */

public class JVMMemoryMonitor implements Runnable {

    private TraceFileLogger logger;
    private Thread thread = null;

    private static JVMMemoryMonitor instance = null;

    private JVMMemoryMonitor() {

    }

    public static JVMMemoryMonitor getInstance() {
        if(instance == null) {
            instance = new JVMMemoryMonitor();
        }
        return instance;
    }

    public void setLogger(TraceFileLogger logger) {
        this.logger = logger;
    }

    @Override
    public void run() {
        while(true) {
            try {
                MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
                long used = memoryMXBean.getHeapMemoryUsage().getUsed() / (1024 * 1024);
                long max = memoryMXBean.getHeapMemoryUsage().getMax() / (1024 * 1024);
                long committed = memoryMXBean.getHeapMemoryUsage().getCommitted() / (1024 * 1024);
                String memoryString = "{USED: " + used + "MB | COMMITTED: " + committed + "MB | MAX: " + max + "MB}";
                logger.trace(memoryString);
                Thread.sleep(5000l);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    public void execute() {
        logger.trace("Starting JVM memory monitor");
        thread = new Thread(this, "monarch-jvmmemory");
        thread.setDaemon(true);
        thread.start();
    }

    public void shutdown() {
        if(thread != null) {
            logger.trace("Shutting down JVM memory monitor");
            thread.interrupt();
        }
    }
}
