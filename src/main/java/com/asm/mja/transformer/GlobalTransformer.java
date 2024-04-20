package com.asm.mja.transformer;

import com.asm.mja.config.Config;
import com.asm.mja.exception.UnsupportedActionException;
import com.asm.mja.filter.Filter;
import com.asm.mja.logging.TraceFileLogger;
import com.asm.mja.utils.ClassLoaderTracer;
import javassist.*;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * The GlobalTransformer class implements the ClassFileTransformer interface
 * to perform bytecode transformation on loaded classes.
 * It is responsible for applying transformations based on the provided configuration.
 *
 * @author ashut
 * @since 11-04-2024
 */
public class GlobalTransformer implements ClassFileTransformer {

    private String configFile;
    private Config config;

    private List<Filter> filters;

    private TraceFileLogger logger;

    private HashSet<String> classesTransformed = new HashSet<>();
    /**
     * Constructs a GlobalTransformer with the specified configuration.
     *
     * @param configFile The path to the configuration file.
     * @param config     The configuration object.
     */
    public GlobalTransformer(String configFile, Config config, TraceFileLogger logger, List<Filter> filters) {
        this.configFile = configFile;
        this.config = config;
        this.logger = logger;
        this.filters = filters;
    }

    /**
     * Transforms the bytecode of a loaded class.
     *
     * @param loader              The classloader loading the class.
     * @param className           The name of the class being transformed.
     * @param classBeingRedefined The class being redefined, if applicable.
     * @param protectionDomain    The protection domain of the class.
     * @param classfileBuffer     The bytecode of the class.
     * @return The transformed bytecode.
     * @throws IllegalClassFormatException If the class file format is illegal or unsupported.
     */
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if(config.isPrintClassLoaderTrace()) {
            logger.trace(ClassLoaderTracer.printClassInfo(className, loader, protectionDomain));
        }
        String formattedClassName = className.replace("/", ".");
        try {
            return transformClass(loader, formattedClassName, classBeingRedefined, classfileBuffer, filters);
        } catch (IOException | CannotCompileException | UnsupportedActionException e) {
            logger.exception(e);
            logger.error("Failed to transform class " + formattedClassName);
        }
        return classfileBuffer;
    }

    public byte[] transformClass(ClassLoader loader, String formattedClassName,
                                        Class<?> classBeingRedefined, byte[] classfileBuffer, List<Filter> filters) throws IOException, CannotCompileException, UnsupportedActionException {
        byte[] modified = classfileBuffer;
        for(Filter filter: filters) {
            if(filter.getClassName().equalsIgnoreCase(formattedClassName)) {
                modified = transformClass(loader, formattedClassName,
                        classBeingRedefined, modified, filter);
            }

        }
        return modified;
    }

    private byte[] transformClass(ClassLoader loader, String formattedClassName,
                                  Class<?> classBeingRedefined, byte[] classfileBuffer, Filter filter) throws IOException, CannotCompileException, UnsupportedActionException {
        byte[] modifiedBytes = classfileBuffer;
        if(classesTransformed.contains(formattedClassName)) {
            logger.trace("Re-transforming class " + formattedClassName);
        } else {
            logger.trace("Going to transform class " + formattedClassName);
            classesTransformed.add(formattedClassName);
        }


        switch (filter.getEvent()) {
            case ENTRY:
                modifiedBytes = performEntryAction(filter.getMethodName(), filter.getAction(), loader, formattedClassName, classBeingRedefined, modifiedBytes, 0);
                break;
            case EXIT:
                modifiedBytes = performExitAction(filter.getMethodName(), filter.getAction(), loader, formattedClassName, classBeingRedefined, modifiedBytes, 0);
                break;
            case AT:
                modifiedBytes = performAtAction(filter.getMethodName(), filter.getAction(), loader, formattedClassName, classBeingRedefined, modifiedBytes, filter.getLineNumber());
        }
        return modifiedBytes;
    }

    private byte[] performAtAction(String methodName, Action action, ClassLoader loader,
                                   String formattedClassName, Class<?> classBeingRedefined, byte[] modifiedBytes, int lineNumber) throws IOException, CannotCompileException {
        switch (action) {
            case STACK:
                return getStack(methodName, Event.AT, loader, formattedClassName, classBeingRedefined, modifiedBytes, lineNumber);
            case HEAP:
                break;
            case ADD:
        }
        return modifiedBytes;
    }

    private byte[] performExitAction(String methodName, Action action, ClassLoader loader,
                                     String formattedClassName, Class<?> classBeingRedefined, byte[] modifiedBytes, int lineNumber) throws IOException, CannotCompileException {
        switch (action) {
            case STACK:
                return getStack(methodName, Event.EXIT, loader, formattedClassName, classBeingRedefined, modifiedBytes, lineNumber);
            case HEAP:
                break;
            case ARGS:
                break;
            case ADD:
        }
        return modifiedBytes;
    }

    private byte[] performEntryAction(String methodName, Action action, ClassLoader loader,
                                      String formattedClassName, Class<?> classBeingRedefined, byte[] modifiedBytes, int lineNumber) throws IOException, CannotCompileException, UnsupportedActionException {
        switch (action) {
            case STACK:
                return getStack(methodName, Event.ENTRY, loader, formattedClassName, classBeingRedefined, modifiedBytes, lineNumber);
            case HEAP:
                break;
            case ARGS:
                return getArgs(methodName, Event.ENTRY, loader, formattedClassName, classBeingRedefined, modifiedBytes);
            case ADD:
        }
        return modifiedBytes;
    }

    private byte[] getArgs(String methodName, Event event, ClassLoader loader, String formattedClassName, Class<?> classBeingRedefined, byte[] modifiedBytes) throws IOException, CannotCompileException, UnsupportedActionException {
        ClassPool pool = ClassPool.getDefault();
        CtClass ctClass = pool.makeClass(new java.io.ByteArrayInputStream(modifiedBytes));
        addLoggerField(ctClass);

        for (CtMethod method : ctClass.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                StringBuilder code = new StringBuilder();
                CtClass[] parameterTypes = new CtClass[0];
                try {
                    parameterTypes = method.getParameterTypes();
                } catch (NotFoundException ignored) {
                }

                if (parameterTypes.length == 0) {
                    code.append("logger.trace(\"[").append(formattedClassName).append(".").append(methodName).append("] | ").append(event).append(" | ").append("ARGS | NULL\");");
                } else {
                    code.append("StringBuilder args = new StringBuilder(\"\");");
                    for (int i = 0; i < parameterTypes.length; i++) {
                        code.append("args.append(\" ").append(i).append("=\").append(");
                        if (parameterTypes[i].isPrimitive()) {
                            code.append("$").append(i + 1);
                        } else {
                            code.append("$").append(i + 1).append(".toString()");
                        }
                        code.append(");");
                    }
                    code.append("logger.trace(\"[").append(formattedClassName).append(".").append(methodName).append("] | ").append(event).append(" | ARGS | \" + args.toString());");
                }

                if (event.equals(Event.ENTRY)) {
                    method.insertBefore(code.toString());
                } else if (event.equals(Event.EXIT)) {
                    throw new UnsupportedActionException("Getting arguments for EXIT is not supported");
                } else {
                    throw new UnsupportedActionException("Getting arguments for AT is not supported");
                }
            }
        }

        modifiedBytes = ctClass.toBytecode();
        ctClass.detach();
        return modifiedBytes;
    }


    private byte[] getStack(String methodName, Event event, ClassLoader loader,
                            String formattedClassName, Class<?> classBeingRedefined, byte[] modifiedBytes, int lineNumber) throws IOException, CannotCompileException {
        ClassPool pool = ClassPool.getDefault();
        CtClass ctClass = pool.makeClass(new java.io.ByteArrayInputStream(modifiedBytes));
        addLoggerField(ctClass);
        for(CtMethod method : ctClass.getDeclaredMethods()) {
            if(method.getName().equals(methodName)) {
                String insertString = "logger.stack(\"{" + formattedClassName + "." + methodName + "} | " + event + " | " + "STACK\"" + ",new Throwable().getStackTrace());";
                if(event.equals(Event.ENTRY))
                    method.insertBefore(insertString);
                else if(event.equals(Event.EXIT))
                    method.insertAfter(insertString);
                else
                    method.insertAt(lineNumber, insertString);
            }
        }
        modifiedBytes = ctClass.toBytecode();
        ctClass.detach();
        return modifiedBytes;
    }

    // Adding this to ensure we don't hit duplicate field issue
    private void addLoggerField(CtClass ctClass) throws CannotCompileException {
        try {
            CtField existingField = ctClass.getField("logger");
            if (existingField != null) {
                // Logger field already exists in the class
                return;
            }
        } catch (NotFoundException ignored) {
            // logger field isn't found
        }

        CtField loggerField = CtField.make("private com.asm.mja.logging.TraceFileLogger logger = com.asm.mja.logging.TraceFileLogger.getInstance();", ctClass);
        ctClass.addField(loggerField);
    }
}
