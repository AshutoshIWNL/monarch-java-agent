package com.asm.mja.attach;

import com.sun.tools.attach.VirtualMachine;
import org.apache.commons.cli.*;

import java.io.File;
import java.lang.management.ManagementFactory;
/**
 * @author ashut
 * @since 05-05-2024
 */

public class AgentAttachCLI {
    public static void main(String[] args) {
        Options options = new Options();
        options.addOption("agentJar", true, "Path to the agent JAR file");
        options.addOption("configFile", true, "Path to the agent config file");
        options.addOption("args", true, "Arguments to pass to the agent");
        options.addOption("pid", true, "PID of the target JVM");

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            String agentFilePath = cmd.getOptionValue("agentJar");
            String configFile = cmd.getOptionValue("configFile");
            String agentArgs = cmd.getOptionValue("args");
            String targetPID = cmd.getOptionValue("pid", getTargetPID());
            attachAgent(agentFilePath, configFile, agentArgs, targetPID);
        } catch (ParseException e) {
            System.err.println("Error parsing command-line arguments: " + e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar AgentAttacherCLI.jar", options);
        } catch (Exception e) {
            System.err.println("Error attaching agent: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void attachAgent(String agentFilePath, String configFile, String agentArgs, String targetPID) throws Exception {
        VirtualMachine vm = VirtualMachine.attach(targetPID);
        String args = "configFile=" + configFile + "," + agentArgs;
        vm.loadAgent(new File(agentFilePath).getAbsolutePath(), args);
        vm.detach();
        System.out.println("Agent attached successfully to PID " + targetPID);
    }

    private static String getTargetPID() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        return name.split("@")[0];
    }
}
