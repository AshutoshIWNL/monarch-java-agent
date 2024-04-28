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

## Usage

To use Monarch-Java-Agent, follow these steps:

1. Build the agent JAR file.
2. Attach the agent to your Java application using the `-javaagent` option.
3. Specify the configuration file and other options as needed.

Example command to attach the agent:

```bash 
java -javaagent:/path/to/monarch-java-agent.jar=configFile=/path/to/config.json,agentLogFileDir=/path/to/log/dir,agentLogLevel=DEBUG YourMainClass
```

## Configuration

You can find the sample configuration file under the sampleConfig directory. 