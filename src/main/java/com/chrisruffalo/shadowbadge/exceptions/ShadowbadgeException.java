package com.chrisruffalo.shadowbadge.exceptions;

public class ShadowbadgeException extends Exception {

    public ShadowbadgeException(String message, Object... params) {
        super(String.format(message, params));
    }
}
