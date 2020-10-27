TRUNCATE TABLE match_cache;

ALTER TABLE match_cache -- Convert to jsonb for better process time
    ALTER COLUMN mCatch_savedMatch
    SET DATA TYPE jsonb
    USING mCatch_savedMatch::jsonb;
    
CREATE INDEX index_matchcache_championId ON match_cache USING gin ((mCatch_savedMatch -> 'championId'));
CREATE INDEX index_matchcache_queueId ON match_cache USING gin ((mCatch_savedMatch -> 'queueId'));
CREATE INDEX index_matchcache_gameVersion ON match_cache USING gin ((mCatch_savedMatch -> 'gameVersion'));

CREATE TABLE champion_role_analysis (
  cra_id 										SERIAL,
  cra_keyChampion								BIGINT 				NOT NULL UNIQUE,
  cra_lastRefresh 								TIMESTAMP			WITHOUT TIME ZONE,
  cra_roles										VARCHAR 			NOT NULL,
  cra_roles_stats								VARCHAR				NOT NULL
);

ALTER TABLE ONLY champion_role_analysis
  ADD CONSTRAINT champion_role_analysis_pkey PRIMARY KEY (cra_id);

CREATE TABLE clash_channel (
	clashChannel_id								SERIAL,
	clashChannel_fk_server						BIGINT				NOT NULL,
	clashChannel_channelId						BIGINT				NOT NULL,
	clashChannel_data							jsonb				NOT NULL,
	clashChannel_timezone						VARCHAR				NOT NULL
);

ALTER TABLE ONLY clash_channel
  ADD CONSTRAINT clash_channel_pkey PRIMARY KEY (clashChannel_id);
  
ALTER TABLE clash_channel
  ADD CONSTRAINT clash_channel_fk_server_const
  FOREIGN KEY (clash_channel_fk_server) REFERENCES server (serv_id);