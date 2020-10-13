TRUNCATE TABLE match_cache;

ALTER TABLE match_cache -- Convert to jsonb for better process time
    ALTER COLUMN mCatch_savedMatch
    SET DATA TYPE jsonb
    USING mCatch_savedMatch::jsonb;
    
CREATE INDEX index_matchcache_championId ON match_cache USING gin ((match_cache ->> 'championId'));