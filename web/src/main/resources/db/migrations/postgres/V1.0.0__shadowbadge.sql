-- create user table
CREATE TABLE "consumers" (
    id TEXT,
    email TEXT,
    authId TEXT,
    displayName TEXT,
    PRIMARY KEY(id)
);

-- create badge + badgeInfo table
CREATE TABLE "badges" (
    -- badge
    id TEXT,
    badgeId TEXT UNIQUE,
    ownerId TEXT,
    displayName TEXT,
    -- badge info
    heading TEXT,
    location TEXT,
    groupName TEXT,
    title TEXT,
    tagline TEXT,
    PRIMARY KEY(id)
);
