-- create badge + badgeInfo table
CREATE TABLE badges (
    -- badge
    id VARCHAR,
    badgeId VARCHAR UNIQUE,
    ownerId VARCHAR,
    displayName VARCHAR,
    -- badge info
    heading VARCHAR,
    location VARCHAR,
    groupName VARCHAR,
    title VARCHAR,
    tagline VARCHAR,
    PRIMARY KEY(id)
);
