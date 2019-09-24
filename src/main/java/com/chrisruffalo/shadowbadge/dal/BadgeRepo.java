package com.chrisruffalo.shadowbadge.dal;

import com.chrisruffalo.shadowbadge.exceptions.RepositoryException;
import com.chrisruffalo.shadowbadge.model.Badge;
import com.chrisruffalo.shadowbadge.model.BadgeInfo;
import com.chrisruffalo.shadowbadge.model.ConfigurationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class BadgeRepo extends AbstractRepo<Badge> {

    public static final String REPO = "BADGES";

    private Logger logger = LoggerFactory.getLogger(BadgeRepo.class);

    @Inject
    EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return this.em;
    }

    @Override
    protected String repo() {
        return REPO;
    }

    @Override
    protected Badge get(String id) {
        return this.get(Badge.class, id, Badge.QUERY_GET, Badge.PARAM_ID);
    }

    public Badge getByBadgeId(String badgeId) {
        return this.get(Badge.class, badgeId, Badge.QUERY_GET_BADGE_ID, Badge.PARAM_BADGE_ID);
    }

    /**
     * "Blindly" claims the badge for the given user. There is no check here to see if the user is correct. I'm not sure
     * if that is smart or not but it could easily be implemented.
     *
     * @param badgeId the badge to be claimed
     * @param ownerId the user that will claim the badge
     * @return the updated badge
     */
    @Transactional
    public Badge claim(final String badgeId, final String ownerId) throws RepositoryException {
        if (null == badgeId || badgeId.isEmpty()) {
            throw new RepositoryException(REPO, "Cannot claim a badge without specifying a badge id");
        }

        if (null == ownerId || ownerId.isEmpty()) {
            throw new RepositoryException(REPO, "Cannot claim a badge without specifying a user id");
        }

        // get badge
        Badge badge = this.get(Badge.class, badgeId, Badge.QUERY_GET_BADGE_ID, Badge.PARAM_BADGE_ID);
        if (null == badge) {

            // create badge
            badge = new Badge();
            badge.setInfo(new BadgeInfo());
            badge.setBadgeId(badgeId);
            badge.setOwnerId(ownerId);
            badge.setStatus(ConfigurationStatus.CLAIMED);

            // persist
            try {
                 em.persist(badge);
            } catch (PersistenceException pex) {
                throw new RepositoryException(REPO, "Could not update badge='%s' claim (reason: %s)", badgeId, pex.getMessage());
            }

            return badge;
        }

        if (null != badge.getOwnerId()) {
            throw new RepositoryException(REPO, "Cannot claim an already-claimed badge");
        }

        // claim badge by setting the owner id
        badge.setOwnerId(ownerId);
        this.em.merge(badge);

        return badge;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<Badge> listForOwner(final String ownerId) {
         return this.list(Badge.class, ownerId, Badge.QUERY_OWNED, Badge.PARAM_OWNER_ID);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public BadgeInfo info(final String badgeId) throws RepositoryException {
        if (null == badgeId || badgeId.isEmpty()) {
            throw new RepositoryException(REPO, "Cannot get info for a badge that does not exist");
        }

        final Badge existingBadge = this.get(Badge.class, badgeId, Badge.QUERY_GET_BADGE_ID, Badge.PARAM_BADGE_ID);
        if (null == existingBadge) {
            return null;
        }

        return existingBadge.getInfo();
    }

    @Transactional
    public Badge updateInfo(final String badgeId, final String ownerId, final BadgeInfo info) throws RepositoryException {
        if (null == badgeId || badgeId.isEmpty()) {
            throw new RepositoryException(REPO, "Cannot modify badge info without providing a badge id");
        }

        if (null == ownerId || ownerId.isEmpty()) {
            throw new RepositoryException(REPO, "A valid owner is necessary to update badge information");
        }

        if (null == info) {
            throw new RepositoryException(REPO, "Cannot update badge info by providing null badge info data");
        }

        final Badge existingBadge = this.get(Badge.class, badgeId, Badge.QUERY_GET_BADGE_ID, Badge.PARAM_BADGE_ID);
        if (null == existingBadge) {
            throw new RepositoryException(REPO, "Cannot update a non-existent badge");
        }

        if (!existingBadge.getOwnerId().equalsIgnoreCase(ownerId)) {
            throw new RepositoryException(REPO, "Cannot update badge info from a different user id");
        }

        existingBadge.setStatus(ConfigurationStatus.CONFIGURED);
        existingBadge.setInfo(info);

        try {
            this.em.merge(existingBadge);
        } catch (PersistenceException ex) {
            throw new RepositoryException(REPO, "Could not save badge info: {}", ex.getMessage());
        }

        logger.info("Updated badge information for badge='{}'", badgeId);

        return existingBadge;
    }

    @Transactional
    public void unclaim(final String badgeId, final String ownerId) throws RepositoryException{
        if (null == badgeId || badgeId.isEmpty()) {
            throw new RepositoryException(REPO, "Cannot modify badge without providing a badge id");
        }

        if (null == ownerId || ownerId.isEmpty()) {
            throw new RepositoryException(REPO, "A valid owner is necessary to remove a badge");
        }

        final Badge existingBadge = this.get(Badge.class, badgeId, Badge.QUERY_GET_BADGE_ID, Badge.PARAM_BADGE_ID);
        if (null == existingBadge) {
            throw new RepositoryException(REPO, "Cannot unclaim a non-existent badge");
        }

        if (!ownerId.equalsIgnoreCase(existingBadge.getOwnerId())) {
            throw new RepositoryException(REPO, "Cannot unclaim a badge you do not own");
        }

        // change badge id and reset configuration status
        existingBadge.setStatus(ConfigurationStatus.NONE);
        existingBadge.setOwnerId(null);

        try {
            this.em.merge(existingBadge);
        } catch (PersistenceException ex) {
            throw new RepositoryException(REPO, "Could not save badge info: {}", ex.getMessage());
        }

        logger.info("Unclaimed badge='{}'", badgeId);
    }
}
