CREATE INDEX idx_match_cache_creationTime
  ON match_cache(mCatch_creationTime);
  
DROP INDEX index_matchcache_championId;
DROP INDEX index_matchcache_queueId;
DROP INDEX index_matchcache_gameVersion;

CREATE INDEX idx_match_cache_player_gameVersion_hash 
ON match_cache USING HASH ((mCatch_savedMatch -> 'gameVersion'));

CREATE INDEX idx_match_cache_player_queueId_hash 
ON match_cache USING HASH ((mCatch_savedMatch -> 'queueId'));

CREATE INDEX idx_match_cache
ON match_cache USING gin (mCatch_savedMatch jsonb_path_ops);