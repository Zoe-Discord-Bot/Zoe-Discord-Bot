TRUNCATE TABLE match_cache;

ALTER TABLE match_cache 
ALTER COLUMN mCatch_gameId TYPE VARCHAR;

TRUNCATE TABLE summoner_cache; --! we save some new data