package com.asm.mja.filter;

import com.asm.mja.transformer.Action;
import com.asm.mja.transformer.Event;

/**
 * @author ashut
 * @since 20-04-2024
 */

public class Filter {
    private String className;
    private String methodName;
    private Event event;
    private Action action;

    private int lineNumber;

    public Filter(String className, String methodName, Event event, Action action, int lineNumber) {
        this.className = className;
        this.methodName = methodName;
        this.event = event;
        this.action = action;
        this.lineNumber = lineNumber;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }
}
