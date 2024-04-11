package com.asm.mja.logging;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author ashut
 * @since 11-04-2024
 */

public class AgentLogger {

    private static PrintWriter writer;
    private static LogLevel globalLogLevel;

    public static void init(String logFilePath, LogLevel logLevel) {
        globalLogLevel = logLevel;
        try {
            writer = new PrintWriter(new FileWriter(logFilePath, true));
        } catch (IOException e) {
            System.err.println("Error opening log file: " + e.getMessage());
        }
    }

    public static void log(String message, LogLevel logLevel) {
        if (logLevel.ordinal() < globalLogLevel.ordinal()) {
            return; // Skip logging if log level is lower than global log level
        }
        if (writer == null) {
            System.err.println("Error: Log file writer is not initialized. Call init() method first.");
            return;
        }
        writer.println("[" + logLevel.name() + "] " + message);
        writer.flush(); // Ensure the message is written immediately
    }

    // For drawing banner
    public static void draw(String message) {
        if (writer == null) {
            System.err.println("Error: Log file writer is not initialized. Call init() method first.");
            return;
        }
        writer.println(message);
        writer.flush(); // Ensure the message is written immediately
    }

    public static void trace(String message) {
        log(message, LogLevel.DEBUG);
    }
    public static void debug(String message) {
        log(message, LogLevel.DEBUG);
    }

    public static void info(String message) {
        log(message, LogLevel.INFO);
    }

    public static void warning(String message) {
        log(message, LogLevel.WARNING);
    }

    public static void error(String message) {
        log(message, LogLevel.ERROR);
    }

    // Ensure this is called
    public static void close() {
        if (writer != null) {
            writer.close();
        }
    }

    public static void deinit() {
        AgentLogger.debug("Deinitializing the AgentLogger");
        close();
    }


    public static void dumpException(Exception e) {
        StackTraceElement[] stackTraceElements = e.getStackTrace();
        StringBuilder stackTrace = new StringBuilder();
        for(StackTraceElement stackTraceElement: stackTraceElements) {
            stackTrace.append(stackTraceElement).append("\n");
        }
        draw(stackTrace.toString());
    }
}
