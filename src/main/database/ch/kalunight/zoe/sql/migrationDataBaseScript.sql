CREATE TABLE match_cache (
  mCatch_id										SERIAL,
  mCatch_gameId									BIGINT				NOT NULL,
  mCatch_platform								VARCHAR				NOT NULL,
  mCatch_savedMatch								json				NOT NULL,
  mCatch_creationTime							timestamp			NOT NULL
);

ALTER TABLE ONLY match_cache
  ADD CONSTRAINT match_cache_pkey PRIMARY KEY (mCatch_id);

ALTER TABLE match_cache
  ADD UNIQUE (mCatch_gameId, mCatch_platform);

CREATE INDEX idx_match_cache_gameId 
  ON match_cache(mCatch_gameId);

CREATE INDEX idx_match_cache_platform 
  ON match_cache(mCatch_platform);