package com.asm.mja.filter;

import com.asm.mja.transformer.Action;
import com.asm.mja.transformer.Event;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author ashut
 * @since 20-04-2024
 */

public class FilterParser {

    private static final Pattern pattern = Pattern.compile("\\((\\d+)\\)");
    private static final Pattern addPattern = Pattern.compile("\\[([^]]+)\\]");
    public static List<Filter> parseFilters(List<String> filters) {
        return filters.stream()
                    .map(filter -> {
                        String[] parts = filter.split("::|@");

                        String className = parts[0];
                        String methodName = parts[1];
                        String eventString = parts[2];
                        Event event = null;
                        int lineNumber = 0;
                        if(eventString.startsWith("AT")) {
                            event = Event.AT;
                            Matcher matcher = pattern.matcher(eventString);
                            if (matcher.find()) {
                                lineNumber = Integer.parseInt(matcher.group(1));
                            }
                        } else if(eventString.startsWith("PROFILE")) {
                            event = Event.PROFILE;
                            return new Filter(className, methodName, event, null, lineNumber);
                        } else {
                            event = Event.valueOf(eventString);
                        }
                        Action action = Action.valueOf(parts[3]);

                        String customCode = null;
                        if (action == Action.ADD && parts.length > 4) {
                            Matcher matcher = addPattern.matcher(parts[4]);
                            if (matcher.find()) {
                                customCode = matcher.group(1);
                            }
                        }

                        return new Filter(className, methodName, event, action, customCode, lineNumber);
                    })
                    .collect(Collectors.toList());
    }

}
