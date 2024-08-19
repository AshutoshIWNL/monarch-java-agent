package com.asm.mja.utils;

import com.asm.mja.filter.Filter;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author ashut
 * @since 15-08-2024
 */

public class ClassFilterUtils {
    public static Class<?>[] filterClasses(Class<?>[] allLoadedClasses, List<Filter> filters) {
        Set<String> classNamesToInstrument = filters.stream()
                .map(Filter::getClassName)
                .collect(Collectors.toSet());

        return Arrays.stream(allLoadedClasses)
                .filter(clazz -> classNamesToInstrument.contains(clazz.getName()))
                .toArray(Class<?>[]::new);
    }
}
