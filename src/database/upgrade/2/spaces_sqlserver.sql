ALTER TABLE ofSpaces ADD persistenceDuration NVARCHAR(255);

/* Update database version */
UPDATE ofVersion SET version = 2 WHERE name = 'spaces';