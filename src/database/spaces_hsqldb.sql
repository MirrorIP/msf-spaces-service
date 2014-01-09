// $Revision$
// $Date$

CREATE TABLE ofSpaces (
  spaceId               VARCHAR(255)  NOT NULL,
  spaceType             INT           NOT NULL,
  isPersistent          INT           NOT NULL,
  spaceName             VARCHAR(1024) NULL,
  mucJID                VARCHAR(255)  NULL,
  pubsubDomain          VARCHAR(255)  NULL,
  pubsubNode            VARCHAR(255)  NULL,
  persistenceDuration   VARCHAR(255)  NULL,
  CONSTRAINT ofSpaces_pk PRIMARY KEY (spaceId)
);

CREATE TABLE ofSpaceMembers (
  spaceId               VARCHAR(255)  NOT NULL,
  userId                VARCHAR(255)  NOT NULL,
  role                  INT           NOT NULL,
  CONSTRAINT ofSpaceMembers_pk PRIMARY KEY (spaceId, userId)
);

CREATE TABLE ofSpaceModels (
  spaceId               VARCHAR(255)  NOT NULL,
  namespace             VARCHAR(255)  NOT NULL,
  schemaLocation        VARCHAR(1024) NOT NULL,
  CONSTRAINT ofSpaceModels_pk PRIMARY KEY (spaceId, namespace, schemaLocation)
);

INSERT INTO ofVersion (name, version) VALUES ('spaces', 2);
