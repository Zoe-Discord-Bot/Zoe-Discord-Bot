CREATE INDEX idx_match_cache_creationTime
  ON match_cache(mCatch_creationTime);
  
DROP INDEX index_matchcache_championId;
DROP INDEX index_matchcache_queueId;
DROP INDEX index_matchcache_gameVersion;

CREATE INDEX index_matchcache_All ON match_cache USING gin (mCatch_savedMatch jsonb_path_ops);