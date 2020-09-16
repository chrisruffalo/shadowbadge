package com.chrisruffalo.shadowbadge.dal;

import com.chrisruffalo.shadowbadge.exceptions.RepositoryException;
import com.chrisruffalo.shadowbadge.model.BaseEntity;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import java.util.List;

public abstract class AbstractRepo<T extends BaseEntity> {

    protected abstract String repo();
    protected abstract EntityManager getEntityManager();
    protected abstract T get(final String id);

    /**
     * Internal get method
     *
     * @param id
     * @return
     */
    T get(Class<T> ofClass, final String id, final String withNamedQuery, final String idProperty) {
        if (null == id || id.isEmpty()) {
            return null;
        }

        // query
        TypedQuery<T> query = this.getEntityManager().createNamedQuery(withNamedQuery, ofClass)
                .setParameter(idProperty, id);

        // find user
        try {
            return query.getSingleResult();
        } catch (NoResultException nre) {
            // deliberate no-op
        }

        // no user found due to error or other issue
        return null;
    }

    List<T> list(Class<T> ofClass, final String id, final String withNamedQuery, final String idParam) {
        TypedQuery<T> query = this.getEntityManager().createNamedQuery(withNamedQuery, ofClass);
        query.setParameter(idParam, id);
        return query.getResultList();
    }

    List<T> list(Class<T> ofClass, final String withNamedQuery) {
        TypedQuery<T> query = this.getEntityManager().createNamedQuery(withNamedQuery, ofClass);
        return query.getResultList();
    }

    @Transactional
    protected T update(final String id, final T entity) throws RepositoryException {
        if (null == id || id.isEmpty()) {
            throw new RepositoryException(this.repo(), "Cannot update entity with null or empty id");
        }

        if (null == entity) {
            throw new RepositoryException(this.repo(), "A non-null entity must be submitted for update");
        }

        if (!id.equals(entity.getId())) {
            throw new RepositoryException(this.repo(), "Entity id must match id of submitted entity");
        }

        // merge user
        this.getEntityManager().merge(entity);

        // update log
        /*
        this.logger.info("Updated user: id='{}'", entity.getId());
        */

        return entity;
    }


}
