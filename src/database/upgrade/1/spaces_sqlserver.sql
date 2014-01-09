CREATE TABLE ofSpaceModels (
  spaceId               NVARCHAR(255)  NOT NULL,
  namespace             NVARCHAR(255)  NOT NULL,
  schemaLocation        NVARCHAR(1024) NOT NULL,
  CONSTRAINT ofSpaceModels_pk PRIMARY KEY (spaceId, namespace, schemaLocation)
);

/* Update database version */
UPDATE ofVersion SET version = 1 WHERE name = 'spaces';