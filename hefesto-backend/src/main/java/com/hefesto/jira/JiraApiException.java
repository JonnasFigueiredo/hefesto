package com.hefesto.jira;

public class JiraApiException extends RuntimeException {

    private final int httpStatus;

    public JiraApiException(int httpStatus, String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public boolean isUnauthorized() {
        return httpStatus == 401 || httpStatus == 403;
    }

    public boolean isNotFound() {
        return httpStatus == 404;
    }
}
