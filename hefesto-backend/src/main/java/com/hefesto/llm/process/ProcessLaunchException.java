package com.hefesto.llm.process;

public class ProcessLaunchException extends RuntimeException {

    public ProcessLaunchException(String message) {
        super(message);
    }

    public ProcessLaunchException(String message, Throwable cause) {
        super(message, cause);
    }
}
