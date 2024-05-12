# Monarch-Java-Agent

Monarch-Java-Agent is a powerful Java agent for method monitoring and analysis. It offers various features to track method execution time, print stack traces, log method arguments and return values, capture heap dumps, and gather JVM-related information.

## Features

- **Method Execution Time**: Print the time taken for a method's execution.
- **Stack Trace Printing**: Print the stack trace when a certain method is invoked.
- **Argument Logging**: Log the arguments provided to a method.
- **Return Value Logging**: Log the return value of a method.
- **Heap Dump Capture**: Take a heap dump when a method is invoked or exits.
- **System Flags Printing**: Print system flags of the target application.
- **JVM Options Printing**: Print JVM options of the target application.
- **JVM Heap Usage Details**: Print JVM heap usage details of the target application.

## Agent Arguments

When attaching the Monarch-Java-Agent, you can pass the following arguments:

- **configFile**: Path to the configuration file that the agent will use. This file contains settings and configurations for the agent's behavior.
- **agentLogFileDir**: Directory where the agent's initialization logging will be written. This log provides insights into the agent's startup process and any initialization-related activities.
- **agentLogLevel**: Specifies the log level for the agent's log messages. Available log levels include DEBUG, INFO, WARN, and ERROR. Adjusting this setting can help manage the verbosity of the agent's log output.

You can find the runtime attach scripts in the attachScript directory.


## Usage

You can attach Monarch-Java-Agent either during startup or during runtime.

**For startup:**

1. Build the agent JAR file.
2. Start your Java application using the `-javaagent` option.
3. Specify the configuration file and other options as needed.

Example command to attach the agent:

```bash 
java -javaagent:/path/to/monarch-java-agent.jar=configFile=/path/to/config.json,agentLogFileDir=/path/to/log/dir,agentLogLevel=DEBUG YourMainClass
```

**For Runtime:**

1. Build the agent JAR file.
2. Start your application.
3. Run "monarchAgentStart.bat"/"monarchAgentStart.sh" and provide the requested details.

Example command run:
```bash 
C:\Users\ashut\monarch-java-agent\attachScript> .\monarchAgentStart.bat
Enter path to the agent JAR file: C:\Users\ashut\monarch-java-agent\target\monarch-java-agent-1.0-SNAPSHOT.jar
Enter path to the agent config file: C:\Users\ashut\monarch-java-agent\sampleConfig\mConfig.json
Enter arguments to pass to the agent: agentLogFileDir=C:\Users\ashut\manualTesting,agentLogLevel=DEBUG
Enter PID of the target JVM (press Enter to use current JVM): 24300
Agent attached successfully to PID 24300
```

## Configuration

Below is a sample configuration JSON that you can use with Monarch-Java-Agent:

```json
{
  "traceFileLocation": "C:\\TraceDumps",
  "agentFilters": [
    "ClassA::getA@ENTRY::STACK",
    "ClassA::getA@ENTRY::ARGS",
    "ClassA::getA@EXIT::RET",
    "ClassA::getAr@ENTRY::ARGS",
    "ClassA::getAr@ENTRY::STACK",
    "ClassA::getAr@EXIT::STACK",
    "ClassA::getAr@EXIT::RET",
    "ClassB::delayed@PROFILE",
    "ClassB::delayed@ENTRY::HEAP"
  ],
  "printClassLoaderTrace": true,
  "printJVMSystemProperties": true,
  "printEnvironmentVariables": true,
  "printJVMHeapUsage": true,
  "maxHeapDumps": 3
}
```

## Filter Syntax

The filter syntax for Monarch-Java-Agent follows the format:

```plaintext
<FQCN>::<MethodName>@<EVENT>::<ACTION>
```

Where:

- `<FQCN>`: Fully Qualified Class Name.
- `<MethodName>`: Name of the method.
- `<EVENT>`: Event at which the action should be performed. Possible values are:
    - ENTRY
    - EXIT
    - AT
    - PROFILE (Note: PROFILE is a special case and no ACTION is required along with it.)
- `<ACTION>`: Action to be performed. Possible values are:
    - STACK: Print stack trace.
    - HEAP: Capture heap dump.
    - ARGS: Log method arguments.
    - RET: Log method return value.