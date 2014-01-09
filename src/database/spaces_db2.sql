-- $Revision$
-- $Date$

CREATE TABLE ofSpaces (
  spaceId               VARCHAR(255) NOT NULL,
  spaceType             INTEGER      NOT NULL,
  isPersistent          INTEGER      NOT NULL,
  spaceName             VARCHAR(512),
  mucJID                VARCHAR(255),
  pubsubDomain          VARCHAR(255),
  pubsubNode            VARCHAR(255),
  persistenceDuration   VARCHAR(255),
  CONSTRAINT ofSpaces_pk PRIMARY KEY (spaceId)
);

CREATE TABLE ofSpaceMembers (
  spaceId               VARCHAR(255) NOT NULL,
  userId                VARCHAR(255) NOT NULL,
  role                  INTEGER      NOT NULL,
  CONSTRAINT ofSpaceMembers_pk PRIMARY KEY (spaceId, userId)
);

CREATE TABLE ofSpaceModels (
  spaceId               VARCHAR(255) NOT NULL,
  namespace             VARCHAR(255) NOT NULL,
  schemaLocation        VARCHAR(512) NOT NULL,
  CONSTRAINT ofSpaceModels_pk PRIMARY KEY (spaceId, namespace, schemaLocation)
);

INSERT INTO ofVersion (name, version) VALUES ('spaces', 2);
