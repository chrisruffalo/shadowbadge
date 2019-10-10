package com.chrisruffalo.shadowbadge.exceptions;

public class ShadowbadgeException extends Exception {

    public ShadowbadgeException(String message) {
        super(message);
    }

    public ShadowbadgeException(String message, Throwable cause) {
        super(message, cause);
    }

    public ShadowbadgeException(Throwable cause) {
        super(cause);
    }

    protected ShadowbadgeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ShadowbadgeException(String message, Object... params) {
        super(String.format(message, params));
    }

    public ShadowbadgeException(Throwable cause, String message, Object... params) {
        super(String.format(message, params), cause);
    }

}
