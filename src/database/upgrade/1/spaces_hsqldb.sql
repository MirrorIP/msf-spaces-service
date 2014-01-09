CREATE TABLE ofSpaceModels (
  spaceId               VARCHAR(255)  NOT NULL,
  namespace             VARCHAR(255)  NOT NULL,
  schemaLocation        VARCHAR(1024) NOT NULL,
  CONSTRAINT ofSpaceModels_pk PRIMARY KEY (spaceId, namespace, schemaLocation)
);

// Update database version
UPDATE ofVersion SET version = 1 WHERE name = 'spaces';