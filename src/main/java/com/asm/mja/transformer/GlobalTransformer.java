package com.asm.mja.transformer;

import com.asm.mja.config.Config;
import com.asm.mja.exception.BackupCreationException;
import com.asm.mja.exception.TransformException;
import com.asm.mja.exception.UnsupportedActionException;
import com.asm.mja.filter.Filter;
import com.asm.mja.logging.TraceFileLogger;
import com.asm.mja.utils.ClassLoaderTracer;
import javassist.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The GlobalTransformer class implements the ClassFileTransformer interface
 * to perform bytecode transformation on loaded classes.
 * It is responsible for applying transformations based on the provided configuration.
 *
 * @author ashut
 * @since 11-04-2024
 */
public class GlobalTransformer implements ClassFileTransformer {

    private static final int MAX_HEAP_COUNT = 3;
    private Config config;
    private List<Filter> filters;
    private final TraceFileLogger logger;
    private Set<String> classesTransformed = ConcurrentHashMap.newKeySet();
    private Set<String> backupSet = ConcurrentHashMap.newKeySet();

    public void resetConfig(Config config) {
        this.config = config;
    }

    /**
     * Constructs a GlobalTransformer with the specified configuration.
     *
     * @param config     The configuration object.
     */
    public GlobalTransformer(Config config, TraceFileLogger logger, List<Filter> filters) {
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
        if(filters.isEmpty())
            return classfileBuffer;
        String formattedClassName = className.replace("/", ".");
        List<Filter> appropriateFilters = getAppropriateFilters(formattedClassName);
        boolean needsInstrumentation =  !appropriateFilters.isEmpty();
        try {
            if(needsInstrumentation) {
                if(!backupSet.contains(formattedClassName))
                    backupByteCode(formattedClassName, classfileBuffer, logger.getTraceDir());
                return transformClass(loader, formattedClassName, classBeingRedefined, classfileBuffer, appropriateFilters);
            }
        } catch (TransformException e) {
            logger.error("Failed to transform class " + formattedClassName, e);
        } catch (BackupCreationException e) {
            logger.error("Failed to back up bytecode for class " + formattedClassName + ", won't go ahead with the transformation", e);
        }
        return classfileBuffer;
    }

    private List<Filter> getAppropriateFilters(String formattedClassName) {
        List<Filter> filterList = new ArrayList<>();
        for(Filter filter: filters) {
            if(filter.getClassName().equalsIgnoreCase(formattedClassName)) {
                filterList.add(filter);
            }
        }
        return filterList;
    }

    public void resetClassesTransformed() {
        this.classesTransformed.clear();
    }

    public void resetFilters() {
        this.filters.clear();
    }

    public void setFilters(List<Filter> filters) {
        this.filters = filters;
    }

    private void backupByteCode(String formattedClassName, byte[] classFileBuffer, String traceDir) throws BackupCreationException {
        File backUpDir = new File(traceDir + File.separator + "backup");
        backUpDir.mkdirs();

        File classFile = new File(backUpDir, formattedClassName.substring(formattedClassName.lastIndexOf('.') + 1) + ".class");

        try (FileOutputStream fos = new FileOutputStream(classFile)) {
            fos.write(classFileBuffer);
            backupSet.add(formattedClassName);
        } catch (IOException e) {
            throw new BackupCreationException(e.getMessage(), e);
        }
    }

    private byte[] transformClass(ClassLoader loader, String formattedClassName,
                                  Class<?> classBeingRedefined, byte[] classfileBuffer, List<Filter> filters) throws TransformException {
        byte[] modifiedBytes = classfileBuffer;
        if(classesTransformed.contains(formattedClassName)) {
            logger.trace("Re-transforming class " + formattedClassName);
        } else {
            logger.trace("Going to transform class " + formattedClassName);
            classesTransformed.add(formattedClassName);
        }
        for(Filter filter: filters) {
            try {
                switch (filter.getEvent()) {
                    case ENTRY:
                        modifiedBytes = performEntryAction(filter.getMethodName(), filter.getAction(), filter.getCustomCode(), loader, formattedClassName, classBeingRedefined, modifiedBytes);
                        break;
                    case EXIT:
                        modifiedBytes = performExitAction(filter.getMethodName(), filter.getAction(), filter.getCustomCode(), loader, formattedClassName, classBeingRedefined, modifiedBytes);
                        break;
                    case AT:
                        modifiedBytes = performAtAction(filter.getMethodName(), filter.getAction(), filter.getCustomCode(), loader, formattedClassName, classBeingRedefined, modifiedBytes, filter.getLineNumber());
                        break;
                    case PROFILE:
                        modifiedBytes = performProfiling(filter.getMethodName(), filter.getAction(), loader, formattedClassName, classBeingRedefined, modifiedBytes, filter.getLineNumber());
                }
            } catch (IOException | CannotCompileException | UnsupportedActionException e) {
                logger.error(e.getMessage(), e);
                throw new TransformException(e);
            }
        }
        return modifiedBytes;
    }

    private byte[] performProfiling(String methodName, Action action, ClassLoader loader,
                                    String formattedClassName, Class<?> classBeingRedefined, byte[] modifiedBytes, int lineNumber) throws IOException, CannotCompileException {
        ClassPool pool = ClassPool.getDefault();
        CtClass ctClass = pool.makeClass(new java.io.ByteArrayInputStream(modifiedBytes));
        //addLoggerField(ctClass);
        for(CtMethod method : ctClass.getDeclaredMethods()) {
            if(method.getName().equals(methodName)) {
                // Declaring startTime as local variable to pass it to insertAfter (it won't work without this)
                method.addLocalVariable("startTime", CtClass.longType);
                method.insertBefore("try { startTime = System.nanoTime(); } catch(Exception e){}");

                method.insertAfter("com.asm.mja.logging.TraceFileLogger logger = com.asm.mja.logging.TraceFileLogger.getInstance(); try {" +
                        "    long endTime = System.nanoTime();" +
                        "    final long executionTime = (endTime - startTime) / 1000000;" +
                        "    logger.trace(\"{" + formattedClassName + '.' + methodName + "} | PROFILE | Execution time: \" + executionTime + \"ms\");" +
                        "} catch (Exception e) { }");

            }
        }
        // CtClass frozen - due to  writeFile()/toClass()/toBytecode()
        modifiedBytes = ctClass.toBytecode();

        // To remove from ClassPool
        ctClass.detach();
        return modifiedBytes;
    }

    private byte[] performAtAction(String methodName, Action action, String customCode, ClassLoader loader,
                                   String formattedClassName, Class<?> classBeingRedefined, byte[] modifiedBytes, int lineNumber) throws IOException, CannotCompileException {
        switch (action) {
            case STACK:
                return getStack(methodName, Event.AT, loader, formattedClassName, classBeingRedefined, modifiedBytes, lineNumber);
            case HEAP:
                return getHeap(methodName, Event.AT, loader, formattedClassName, classBeingRedefined, modifiedBytes, lineNumber);
            case ADD:
                return addCustomCode(customCode, methodName, Event.AT, loader, formattedClassName, classBeingRedefined, modifiedBytes, lineNumber);
        }
        return modifiedBytes;
    }

    private byte[] performExitAction(String methodName, Action action, String customCode, ClassLoader loader,
                                     String formattedClassName, Class<?> classBeingRedefined, byte[] modifiedBytes) throws IOException, CannotCompileException, UnsupportedActionException {
        switch (action) {
            case STACK:
                return getStack(methodName, Event.EXIT, loader, formattedClassName, classBeingRedefined, modifiedBytes, 0);
            case HEAP:
                return getHeap(methodName, Event.EXIT, loader, formattedClassName, classBeingRedefined, modifiedBytes, 0);
            case RET:
                return getReturnValue(methodName, Event.EXIT, loader, formattedClassName, classBeingRedefined, modifiedBytes);
            case ADD:
                return addCustomCode(customCode, methodName, Event.EXIT, loader, formattedClassName, classBeingRedefined, modifiedBytes, 0);
        }
        return modifiedBytes;
    }

    private byte[] performEntryAction(String methodName, Action action, String customCode, ClassLoader loader,
                                      String formattedClassName, Class<?> classBeingRedefined, byte[] modifiedBytes) throws IOException, CannotCompileException, UnsupportedActionException {
        switch (action) {
            case STACK:
                return getStack(methodName, Event.ENTRY, loader, formattedClassName, classBeingRedefined, modifiedBytes, 0);
            case HEAP:
                return getHeap(methodName, Event.ENTRY, loader, formattedClassName, classBeingRedefined, modifiedBytes, 0);
            case ARGS:
                return getArgs(methodName, Event.ENTRY, loader, formattedClassName, classBeingRedefined, modifiedBytes);
            case ADD:
                return addCustomCode(customCode, methodName, Event.ENTRY, loader, formattedClassName, classBeingRedefined, modifiedBytes, 0);
        }
        return modifiedBytes;
    }

    private byte[] getArgs(String methodName, Event event, ClassLoader loader, String formattedClassName,
                           Class<?> classBeingRedefined, byte[] modifiedBytes) throws IOException, CannotCompileException, UnsupportedActionException {
        ClassPool pool = ClassPool.getDefault();
        CtClass ctClass = pool.makeClass(new java.io.ByteArrayInputStream(modifiedBytes));
        //addLoggerField(ctClass);

        for (CtMethod method : ctClass.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                StringBuilder code = new StringBuilder();
                CtClass[] parameterTypes = new CtClass[0];
                try {
                    parameterTypes = method.getParameterTypes();
                } catch (NotFoundException ignored) {
                    //ignoring this
                }

                if (parameterTypes.length == 0) {
                    code.append("com.asm.mja.logging.TraceFileLogger logger = com.asm.mja.logging.TraceFileLogger.getInstance();");
                    code.append("try {");
                    code.append("    logger.trace(\"{").append(formattedClassName).append('.').append(methodName).append("} | ").append(event).append(" | ").append("ARGS | NULL\");");
                    code.append("} catch (Exception e) {}");
                } else {
                    code.append("com.asm.mja.logging.TraceFileLogger logger = com.asm.mja.logging.TraceFileLogger.getInstance();");
                    code.append("try {");
                    code.append("    StringBuilder args = new StringBuilder(\"\");");
                    for (int i = 0; i < parameterTypes.length; i++) {
                        code.append("    args.append(\" ").append(i).append("=\").append(");
                        if (parameterTypes[i].isPrimitive()) {
                            code.append('$').append(i + 1);
                        } else {
                            code.append('$').append(i + 1).append(".toString()");
                        }
                        code.append(");");
                    }
                    code.append("    logger.trace(\"{").append(formattedClassName).append('.').append(methodName).append("} | ").append(event).append(" | ARGS | \" + args.toString());");
                    code.append("} catch (Exception e) {}");
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

        // CtClass frozen - due to  writeFile()/toClass()/toBytecode()
        modifiedBytes = ctClass.toBytecode();

        // To remove from ClassPool
        ctClass.detach();
        return modifiedBytes;
    }


    private byte[] getStack(String methodName, Event event, ClassLoader loader,
                            String formattedClassName, Class<?> classBeingRedefined, byte[] modifiedBytes, int lineNumber) throws IOException, CannotCompileException {
        ClassPool pool = ClassPool.getDefault();
        CtClass ctClass = pool.makeClass(new java.io.ByteArrayInputStream(modifiedBytes));
        //addLoggerField(ctClass);
        for(CtMethod method : ctClass.getDeclaredMethods()) {
            if(method.getName().equals(methodName)) {
                String insertString = "com.asm.mja.logging.TraceFileLogger logger = com.asm.mja.logging.TraceFileLogger.getInstance(); try { " +
                        "logger.stack(\"{" + formattedClassName + '.' + methodName + "} | " + event + " | " + "STACK\"" + ", new Throwable().getStackTrace()); " +
                        "} catch (Exception e) {}";
                if(event.equals(Event.ENTRY))
                    method.insertBefore(insertString);
                else if(event.equals(Event.EXIT))
                    method.insertAfter(insertString);
                else
                    method.insertAt(lineNumber, insertString);
            }
        }
        // CtClass frozen - due to  writeFile()/toClass()/toBytecode()
        modifiedBytes = ctClass.toBytecode();

        // To remove from ClassPool
        ctClass.detach();
        return modifiedBytes;
    }

    private byte[] getHeap(String methodName, Event event, ClassLoader loader,
                           String formattedClassName, Class<?> classBeingRedefined, byte[] modifiedBytes, int lineNumber) throws IOException, CannotCompileException {
        ClassPool pool = ClassPool.getDefault();
        CtClass ctClass = pool.makeClass(new java.io.ByteArrayInputStream(modifiedBytes));
        //addLoggerField(ctClass);
        for (CtMethod method : ctClass.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                String insertString = "com.asm.mja.logging.TraceFileLogger logger = com.asm.mja.logging.TraceFileLogger.getInstance(); try { " +
                        "com.asm.mja.utils.HeapDumpUtils.collectHeap();" +
                        "logger.trace(\"{" + formattedClassName + '.' + methodName + "} | " + event + " | " + "HEAP\"" + "); " +
                        "} catch (Exception e) {}";
                if (event.equals(Event.ENTRY))
                    method.insertBefore(insertString);
                else if (event.equals(Event.EXIT))
                    method.insertAfter(insertString);
                else
                    method.insertAt(lineNumber, insertString);
            }
        }
        // CtClass frozen - due to  writeFile()/toClass()/toBytecode()
        modifiedBytes = ctClass.toBytecode();

        // To remove from ClassPool
        ctClass.detach();
        return modifiedBytes;
    }

    /*
      For ref:
        $_ gives the return value
        $r gives the return type
     */
    private byte[] getReturnValue(String methodName, Event event, ClassLoader loader,
                                  String formattedClassName, Class<?> classBeingRedefined, byte[] modifiedBytes) throws IOException, CannotCompileException, UnsupportedActionException {
        ClassPool pool = ClassPool.getDefault();
        CtClass ctClass = pool.makeClass(new java.io.ByteArrayInputStream(modifiedBytes));
        //addLoggerField(ctClass);

        for (CtMethod method : ctClass.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                StringBuilder code = new StringBuilder();
                CtClass returnType;
                try {
                    returnType = method.getReturnType();
                } catch (NotFoundException e) {
                    throw new UnsupportedActionException(e.getMessage());
                }

                if (event.equals(Event.EXIT)) {
                    String returnVariableName;

                    if (returnType.equals(CtClass.voidType)) {
                        // For void methods, no return value to capture
                        code.append("com.asm.mja.logging.TraceFileLogger logger = com.asm.mja.logging.TraceFileLogger.getInstance();");
                        code.append("logger.trace(\"{").append(formattedClassName).append('.').append(methodName).append("} | ").append(event).append(" | RET | VOID\");");
                    } else if (returnType.isPrimitive()) {
                        // For primitive types, no need to call toString()
                        returnVariableName = "$$_returnValue";
                        code.append(returnType.getName()).append(' ').append(returnVariableName).append(" = ($r) $_;");
                        code.append("com.asm.mja.logging.TraceFileLogger logger = com.asm.mja.logging.TraceFileLogger.getInstance();");
                        code.append("try {");
                        code.append("    logger.trace(\"{").append(formattedClassName).append('.').append(methodName).append("} | ").append(event).append(" | RET | \" + ").append(returnVariableName).append(");");
                        code.append("} catch (Exception e) {}");
                    } else {
                        // For non-primitive types, check for null before calling toString()
                        returnVariableName = "$$_returnValue";
                        code.append(returnType.getName()).append(' ').append(returnVariableName).append(" = ($r) $_;");
                        code.append("com.asm.mja.logging.TraceFileLogger logger = com.asm.mja.logging.TraceFileLogger.getInstance();");
                        code.append("try {");
                        code.append("    if (").append(returnVariableName).append(" != null) {");
                        code.append("        logger.trace(\"{").append(formattedClassName).append('.').append(methodName).append("} | ").append(event).append(" | RET | \" + ").append(returnVariableName).append(".toString());");
                        code.append("    } else {");
                        code.append("        logger.trace(\"{").append(formattedClassName).append('.').append(methodName).append("} | ").append(event).append(" | RET | NULL\");");
                        code.append("    }");
                        code.append("} catch (Exception e) {}");
                    }
                } else {
                    throw new UnsupportedActionException("Getting return value for " + event + " is not supported");
                }

                method.insertAfter(code.toString()); // Insert after to capture return value
            }
        }
        // CtClass frozen - due to  writeFile()/toClass()/toBytecode()
        modifiedBytes = ctClass.toBytecode();

        // To remove from ClassPool
        ctClass.detach();
        return modifiedBytes;
    }

    private byte[] addCustomCode(String customCode, String methodName, Event event, ClassLoader loader,
                                 String formattedClassName, Class<?> classBeingRedefined, byte[] modifiedBytes, int lineNumber) throws IOException, CannotCompileException {
        ClassPool pool = ClassPool.getDefault();
        CtClass ctClass = pool.makeClass(new java.io.ByteArrayInputStream(modifiedBytes));

        for (CtMethod method : ctClass.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                String safeCustomCode = "try { " + customCode + " } catch (Exception e) { " +
                        "com.asm.mja.logging.TraceFileLogger logger = com.asm.mja.logging.TraceFileLogger.getInstance();" +
                        "logger.error(\"Custom code threw an exception in " + formattedClassName + '.' + methodName + ": \" + e.getMessage());" +
                        "}";
                if(event.equals(Event.ENTRY))
                    method.insertBefore(safeCustomCode);
                else if(event.equals(Event.AT))
                    method.insertAt(lineNumber, safeCustomCode);
                else
                    method.insertAfter(safeCustomCode);
            }
        }
        // CtClass frozen - due to  writeFile()/toClass()/toBytecode()
        modifiedBytes = ctClass.toBytecode();

        // To remove from ClassPool
        ctClass.detach();
        return modifiedBytes;
    }
}
