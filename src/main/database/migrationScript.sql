TRUNCATE TABLE match_cache;

ALTER TABLE match_cache -- Convert to jsonb for better process time
    ALTER COLUMN mCatch_savedMatch
    SET DATA TYPE jsonb
    USING mCatch_savedMatch::jsonb;
    
CREATE INDEX index_matchcache_championId ON match_cache USING gin ((mCatch_savedMatch -> 'championId'));

CREATE TABLE champion_role_analysis (
  cra_id 										SERIAL,
  cra_keyChampion								BIGINT 				NOT NULL UNIQUE,
  cra_lastRefresh 								TIMESTAMP			WITHOUT TIME ZONE,
  cra_roles										VARCHAR 			NOT NULL
);

ALTER TABLE ONLY champion_role_analysis
  ADD CONSTRAINT champion_role_analysis_pkey PRIMARY KEY (cra_id);