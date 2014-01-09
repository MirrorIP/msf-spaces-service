/* $Revision$   */
/* $Date$       */

CREATE TABLE ofSpaces (
  spaceId               NVARCHAR(255)  NOT NULL,
  spaceType             INT            NOT NULL,
  isPersistent          INT            NOT NULL,
  spaceName             NVARCHAR(1024) NULL,
  mucJID                NVARCHAR(255)  NULL,
  pubsubDomain          NVARCHAR(255)  NULL,
  pubsubNode            NVARCHAR(255)  NULL,
  persistenceDuration   NVARCHAR(255)  NULL,
  CONSTRAINT ofSpaces_pk PRIMARY KEY (spaceId)
);

CREATE TABLE ofSpaceMembers (
  spaceId               NVARCHAR(255)  NOT NULL,
  userId                NVARCHAR(255)  NOT NULL,
  role                  INT            NOT NULL,
  CONSTRAINT ofSpaceMembers_pk PRIMARY KEY (spaceId, userId)
);

CREATE TABLE ofSpaceModels (
  spaceId               NVARCHAR(255)  NOT NULL,
  namespace             NVARCHAR(255)  NOT NULL,
  schemaLocation        NVARCHAR(1024) NOT NULL,
  CONSTRAINT ofSpaceModels_pk PRIMARY KEY (spaceId, namespace, schemaLocation)
);

INSERT INTO ofVersion (name, version) VALUES ('spaces', 2);