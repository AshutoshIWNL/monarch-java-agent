package com.asm.mja.transformer;

import com.asm.mja.config.Config;
import com.asm.mja.logging.AgentLogger;
import com.asm.mja.utils.ClassLoaderTracer;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

/**
 * @author ashut
 * @since 11-04-2024
 */

public class GlobalTransformer implements ClassFileTransformer {

    String configFile;
    Config config;

    public GlobalTransformer(String configFile, Config config) {
        this.configFile = configFile;
        this.config = config;
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

        if(config.isPrintClassLoaderTrace()) {
            AgentLogger.debug(ClassLoaderTracer.printClassInfo(className, loader, protectionDomain));
        }
        return classfileBuffer;
    }
}
