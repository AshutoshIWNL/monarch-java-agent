package com.asm.mja.transformer;

import com.asm.mja.config.Config;
import com.asm.mja.logging.AgentLogger;
import com.asm.mja.logging.TraceFileLogger;
import com.asm.mja.utils.ClassLoaderTracer;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

/**
 * The GlobalTransformer class implements the ClassFileTransformer interface
 * to perform bytecode transformation on loaded classes.
 * It is responsible for applying transformations based on the provided configuration.
 *
 * @author ashut
 * @since 11-04-2024
 */
public class GlobalTransformer implements ClassFileTransformer {

    String configFile;
    Config config;

    TraceFileLogger logger;
    /**
     * Constructs a GlobalTransformer with the specified configuration.
     *
     * @param configFile The path to the configuration file.
     * @param config     The configuration object.
     */
    public GlobalTransformer(String configFile, Config config, TraceFileLogger logger) {
        this.configFile = configFile;
        this.config = config;
        this.logger = logger;
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
        return classfileBuffer;
    }

}
