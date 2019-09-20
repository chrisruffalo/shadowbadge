package com.chrisruffalo.shadowbadge.exceptions;

public class RepositoryException extends ShadowbadgeException {

    private final String repository;

    public  RepositoryException(final String repo, final String error) {
        super(error);
        this.repository = repo;
    }

    public  RepositoryException(final String repo, final String error, final Object... params) {
        super(error, params);
        this.repository = repo;
    }

    public String getRepository() {
        return repository;
    }

    @Override
    public String getMessage() {
        return String.format("|%s| - %s", this.repository, super.getMessage());
    }
}
