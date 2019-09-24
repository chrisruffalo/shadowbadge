-- create user table
CREATE TABLE "consumers" (
    id VARCHAR,
    email VARCHAR,
    authId VARCHAR,
    displayName VARCHAR,
    PRIMARY KEY(id)
);

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
