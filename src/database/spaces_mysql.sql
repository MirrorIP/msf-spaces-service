# $Revision$
# $Date$

CREATE TABLE ofSpaces (
  spaceId               VARCHAR(255) NOT NULL,
  spaceType             TINYINT      NOT NULL,
  isPersistent          INT          NOT NULL,
  spaceName             VARCHAR(512) NULL,
  mucJID                VARCHAR(255) NULL,
  pubsubDomain          VARCHAR(255) NULL,
  pubsubNode            VARCHAR(255) NULL,
  persistenceDuration   VARCHAR(255) NULL,
  PRIMARY KEY (spaceId)
);

CREATE TABLE ofSpaceMembers (
  spaceId               VARCHAR(255) NOT NULL,
  userId                VARCHAR(255) NOT NULL,
  role                  TINYINT      NOT NULL,
  PRIMARY KEY (spaceId, userId)
);

CREATE TABLE ofSpaceModels (
  spaceId               VARCHAR(255) NOT NULL,
  namespace             VARCHAR(200) NOT NULL,
  schemaLocation        VARCHAR(512) NOT NULL,
  spaceModelId          INTEGER AUTO_INCREMENT,
  PRIMARY KEY (spaceModelId),
  UNIQUE INDEX (spaceId, namespace, schemaLocation)
);

INSERT INTO ofVersion (name, version) VALUES ('spaces', 2);
