CREATE TABLE ofSpaceModels (
  spaceId               VARCHAR(255) NOT NULL,
  namespace             VARCHAR(255) NOT NULL,
  schemaLocation        VARCHAR(512) NOT NULL,
  spaceModelId          INTEGER AUTO_INCREMENT,
  PRIMARY KEY (spaceModelId),
  UNIQUE INDEX (spaceId, namespace, schemaLocation)
);

# Update database version
UPDATE ofVersion SET version = 1 WHERE name = 'spaces';
