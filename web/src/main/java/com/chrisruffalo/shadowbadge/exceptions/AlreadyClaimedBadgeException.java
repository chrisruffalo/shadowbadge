package com.chrisruffalo.shadowbadge.exceptions;

public class AlreadyClaimedBadgeException extends RepositoryException {

    private final String badge;
    private final String owner;

    public AlreadyClaimedBadgeException(String badge, String owner, String message, Object... params) {
        super(message, params);
        this.badge = badge;
        this.owner = owner;
    }

    public String getBadge() {
        return badge;
    }

    public String getOwner() {
        return owner;
    }
}
