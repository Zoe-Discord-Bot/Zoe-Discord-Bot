CREATE TABLE rank_history_channel(
  rhChannel_id										SERIAL,
  rhChannel_fk_server								BIGINT				NOT NULL,
  rhChannel_channelId								BIGINT	
);

CREATE TABLE last_rank(
  lastRank_id										SERIAL,
  lastRank_fk_leagueAccount							BIGINT				NOT NULL,
  lastRank_soloq									json,
  lastRank_flex										json,
  lastRank_tft										json
);

ALTER TABLE rank_history_channel
  ADD CONSTRAINT rank_history_channel_fk_server_const 
  FOREIGN KEY (rhChannel_fk_server) REFERENCES server (serv_id);
  
ALTER TABLE last_rank
  ADD CONSTRAINT last_rank_fk_leagueAccount_const 
  FOREIGN KEY (lastRank_fk_leagueAccount) REFERENCES league_account (leagueAccount_id);