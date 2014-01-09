-- $Revision$
-- $Date$

CREATE TABLE ofSpaces (
  spaceId               VARCHAR2(255)  NOT NULL,
  spaceType             INTEGER        NOT NULL,
  isPersistent          INT            NOT NULL,
  spaceName             VARCHAR2(1024) NULL,
  mucJID                VARCHAR2(255)  NULL,
  pubsubDomain          VARCHAR2(255)  NULL,
  pubsubNode            VARCHAR2(255)  NULL,
  persistenceDuration   VARCHAR2(255)  NULL,
  CONSTRAINT ofSpaces_pk PRIMARY KEY (spaceId)
);

CREATE TABLE ofSpaceMembers (
  spaceId              VARCHAR2(255)  NOT NULL,
  userId               VARCHAR2(255)  NOT NULL,
  role                 INTEGER        NOT NULL,
  CONSTRAINT ofSpaceMembers_pk PRIMARY KEY (spaceId, userId)
);

CREATE TABLE ofSpaceModels (
  spaceId              VARCHAR2(255)  NOT NULL,
  namespace            VARCHAR2(255)  NOT NULL,
  schemaLocation       VARCHAR2(1024) NOT NULL,
  CONSTRAINT ofSpaceModels_pk PRIMARY KEY (spaceId, namespace, schemaLocation)
);

INSERT INTO ofVersion (name, version) VALUES ('spaces', 2);

commit;