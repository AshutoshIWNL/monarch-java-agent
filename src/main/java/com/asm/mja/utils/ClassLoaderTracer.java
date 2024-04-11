package com.asm.mja.utils;

import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;

/**
 * @author ashut
 * @since 11-04-2024
 */

public class ClassLoaderTracer {

    public static String printClassInfo(String className, ClassLoader loader, ProtectionDomain protectionDomain) {
        return "Class: " + className + "\n" +
                printClassLoaderHierarchy(loader) + "\n" +
                printLoadedFrom(protectionDomain);
    }

    public static String printClassLoaderHierarchy(ClassLoader classLoader) {
        StringBuilder hierarchy = new StringBuilder("ClassLoaderHierarchy: ");
        while (classLoader != null) {
            hierarchy.append(classLoader.getClass().getName()).append(" -> ");
            classLoader = classLoader.getParent();
        }
        hierarchy.append("BootstrapClassLoader");
        return hierarchy.toString();
    }

    public static String printLoadedFrom(ProtectionDomain protectionDomain) {
        CodeSource codeSource = protectionDomain.getCodeSource();
        String loadedFrom = "Loader from: ";
        if (codeSource != null) {
            URL location = codeSource.getLocation();
            loadedFrom += location.getPath();
        } else {
            loadedFrom +=  "Unknown location";
        }
        return loadedFrom;
    }
}
