package com.asm.mja.utils;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Properties;

/**
 * @author ashut
 * @since 12-04-2024
 */

public class JVMUtils {
    public static Integer JVMPID = null;

    public static Integer getJVMPID() {
        if(JVMPID == null) {
            String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
            String[] parts = runtimeName.split("@");
            if (parts.length > 0) {
                String pidString = parts[0];
                try {
                    JVMPID = Integer.parseInt(pidString);
                } catch (NumberFormatException e) {
                    throw new IllegalStateException("Failed to parse JVM PID: " + pidString, e);
                }
            } else {
                throw new IllegalStateException("Failed to get JVM PID");
            }
        }
        return JVMPID;
    }

    public static String getJVMSystemProperties() {
        Properties properties = System.getProperties();

        StringBuilder systemProps = new StringBuilder();
        int counter = properties.size();
        systemProps.append("\n\n*******JVM System Properties*******\n");
        for (String key : properties.stringPropertyNames()) {
            String value = properties.getProperty(key);
            if(counter-- != 1)
                systemProps.append(key).append("=").append(value).append("\n");
            else
                systemProps.append(key).append("=").append(value);
        }
        systemProps.append("\n**************************************\n");
        return systemProps.toString();
    }

    public static String getJVMCommandLine() {
        StringBuilder commandLine = new StringBuilder();
        commandLine.append("JVM command line: ");
        List<String> jvmArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();

        for (String argument : jvmArguments) {
            commandLine.append(argument).append(";");
        }
        return commandLine.toString();
    }
}
