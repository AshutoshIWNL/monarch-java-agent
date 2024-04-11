package com.asm.mja.logging;

import com.asm.mja.utils.DateUtils;
import com.asm.mja.utils.JVMUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author ashut
 * @since 11-04-2024
 */

public class TraceFileLogger {
    private String fileName;
    private PrintWriter writer;
    private final Lock lock = new ReentrantLock();

    public TraceFileLogger(String location) {
        fileName = location + File.separator + generateLogFileName() + ".out";
        try {
            writer = new PrintWriter(new FileWriter(fileName, true));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String generateLogFileName() {
        String fileName = "TraceFile_" + JVMUtils.getJVMPID() + "_";
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        fileName += dateFormat.format(new Date());
        return fileName;
    }

    public void trace(String message) {
        if (writer == null) {
            throw new IllegalStateException("TraceFileLogger has not been initialized. Call init() first.");
        }
        StringBuilder logMessage = new StringBuilder();
        logMessage.append(DateUtils.getFormattedTimestamp()).append(" ");
        logMessage.append("[TRACE] ");
        logMessage.append("[").append(Thread.currentThread().getName()).append("] ");
        logMessage.append(message);
        writeLog(logMessage.toString());
    }

    private void writeLog(String message) {
        lock.lock();
        try {
            writer.println(message);
            writer.flush();
        } finally {
            lock.unlock();
        }
    }

    public void close() {
        if (writer != null) {
            writer.close();
        }
    }

}
