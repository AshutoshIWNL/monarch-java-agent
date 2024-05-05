package com.asm.mja.utils;

import com.asm.mja.Agent;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.URLDecoder;
import java.util.jar.JarFile;

/**
 * @author ashut
 * @since 05-05-2024
 */

public class LoaderUtils {
    public static void addToBootClassLoader(Instrumentation instrumentation) {
        try {
            String monarchJarPath = URLDecoder.decode(Agent.class.getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8");

            instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(monarchJarPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
