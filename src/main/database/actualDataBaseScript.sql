-- Init Zoe DB

CREATE DATABASE zoe;
create user zoeadmin with encrypted password 'xf345sD2#a@v'; --Random test password
grant all privileges on database zoe to zoeAdmin;

CREATE TABLE server (
  serv_id 										SERIAL,
  serv_guildId 									BIGINT 				NOT NULL UNIQUE,
  serv_language 								VARCHAR 			NOT NULL,
  serv_lastRefresh 								TIMESTAMP			WITHOUT TIME ZONE
);

CREATE TABLE player (
  player_id 									SERIAL,
  player_fk_server 								BIGINT 				NOT NULL,
  player_fk_team 								BIGINT,
  player_discordId 								BIGINT				NOT NULL,
  player_mentionnable 							boolean
);

CREATE TABLE team (
  team_id 										SERIAL,
  team_fk_server 								BIGINT 				NOT NULL,
  team_name 									VARCHAR 			NOT NULL
);

CREATE TABLE server_status (
  servStatus_id									SERIAL,
  servStatus_fk_server							BIGINT				NOT NULL,
  servStatus_inTreatment						boolean				DEFAULT false
);

CREATE TABLE server_configuration (
  servConfig_id 								SERIAL,
  servConfig_fk_server 							BIGINT 				NOT NULL
);

CREATE TABLE self_adding_option (
  selfOption_id 								SERIAL,
  selfOption_fk_serverConfig 					BIGINT 				NOT NULL,
  selfOption_activate 							boolean 			DEFAULT TRUE
);

CREATE TABLE region_option (
  regionOption_id 								SERIAL,
  regionOption_fk_serverConfig 					BIGINT 				NOT NULL,
  regionOption_region 							VARCHAR
);

CREATE TABLE clean_channel_option (
  cleanOption_id								SERIAL,
  cleanOption_fk_serverConfig					BIGINT				NOT NULL,
  cleanOption_channelId							BIGINT,
  cleanOption_option							VARCHAR
);

CREATE TABLE game_info_card_option (
  gameCardOption_id								SERIAL,
  gameCardOption_fk_serverConfig				BIGINT				NOT NULL,
  gameCardOption_activate						boolean
);

CREATE TABLE role_option (
  roleOption_id									SERIAL,
  roleOption_fk_serverConfig					BIGINT				NOT NULL,
  roleOption_roleId								BIGINT
);

CREATE TABLE info_panel_ranked_option (
  infoPanelRanked_id 								SERIAL,
  infoPanelRanked_fk_serverConfig 					BIGINT 				NOT NULL,
  infoPanelRanked_activate 							boolean 			DEFAULT TRUE
);

CREATE TABLE info_channel (
  infoChannel_id								SERIAL,
  infoChannel_fk_server							BIGINT				NOT NULL,
  infoChannel_channelId							BIGINT
);

CREATE TABLE info_panel_message (
  infoPanel_id									SERIAL,
  infoPanel_fk_infoChannel						BIGINT				NOT NULL,
  infoPanel_messageId							BIGINT				NOT NULL
);

CREATE TABLE game_info_card (
  gameCard_id									SERIAL,
  gameCard_fk_infoChannel						BIGINT				NOT NULL,
  gameCard_fk_currentGame						BIGINT				UNIQUE,
  gameCard_titleMessageId						BIGINT,
  gameCard_infoCardMessageId					BIGINT,
  gameCard_status								VARCHAR,
  gameCard_creationTime							TIMESTAMP			WITHOUT TIME ZONE
);

CREATE TABLE league_account (
  leagueAccount_id								SERIAL,
  leagueAccount_fk_player						BIGINT				NOT NULL,
  leagueAccount_fk_gameCard						BIGINT,
  leagueAccount_fk_currentGame					BIGINT,
  leagueAccount_name							VARCHAR				NOT NULL,
  leagueAccount_summonerId						VARCHAR				NOT NULL,
  leagueAccount_accountId						VARCHAR				NOT NULL,
  leagueAccount_puuid							VARCHAR				NOT NULL,
  leagueAccount_server							VARCHAR				NOT NULL
);

CREATE TABLE current_game_info (
  currentGame_id								SERIAL,
  currentGame_currentGame						JSON
);

CREATE TABLE match_cache (
  mCatch_id										SERIAL,
  mCatch_gameId									BIGINT				NOT NULL,
  mCatch_platform								VARCHAR				NOT NULL,
  mCatch_savedMatch								json				NOT NULL,
  mCatch_creationTime							timestamp			NOT NULL
);

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
  lastRank_tft										json,
  lastRank_soloqSecond 								json,
  lastRank_soloqLastRefresh							TIMESTAMP			WITHOUT TIME ZONE,
  lastRank_flexSecond 								json,
  lastRank_flexLastRefresh							TIMESTAMP			WITHOUT TIME ZONE,
  lastRank_tftSecond 								json,
  lastRank_tftLastRefresh							TIMESTAMP			WITHOUT TIME ZONE
);

CREATE TABLE leaderboard (
  lead_id 										SERIAL,
  lead_fk_server								BIGINT				NOT NULL,
  lead_message_channelId						BIGINT 				NOT NULL,
  lead_message_id								BIGINT 				NOT NULL,
  lead_type		 								BIGINT				NOT NULL,
  lead_data										json,
  lead_lastRefresh 								TIMESTAMP			WITHOUT TIME ZONE
);

CREATE TABLE banned_account (
  banAcc_id										SERIAL,
  banAcc_summonerId								VARCHAR				NOT NULL,
  banAcc_server									VARCHAR				NOT NULL
);


-- Constraints
ALTER TABLE ONLY server
  ADD CONSTRAINT server_pkey PRIMARY KEY (serv_id);
  
ALTER TABLE ONLY player 
  ADD CONSTRAINT player_pkey PRIMARY KEY (player_id);
  
ALTER TABLE ONLY team 
  ADD CONSTRAINT team_pkey PRIMARY KEY (team_id);
  
ALTER TABLE ONLY server_status
  ADD CONSTRAINT server_status_pkey PRIMARY KEY (servStatus_id);
  
ALTER TABLE ONLY server_configuration 
  ADD CONSTRAINT server_config_pkey PRIMARY KEY (servConfig_id);

ALTER TABLE ONLY self_adding_option 
  ADD CONSTRAINT self_adding_option_pkey PRIMARY KEY (selfOption_id);
  
ALTER TABLE ONLY region_option
  ADD CONSTRAINT region_option_pkey PRIMARY KEY (regionOption_id);
  
ALTER TABLE ONLY clean_channel_option
  ADD CONSTRAINT clean_channel_option_pkey PRIMARY KEY (cleanOption_id);

ALTER TABLE ONLY game_info_card_option
  ADD CONSTRAINT game_info_card_option_pkey PRIMARY KEY (gameCardOption_id);
  
ALTER TABLE ONLY role_option
  ADD CONSTRAINT role_option_pkey PRIMARY KEY (roleOption_id);
  
ALTER TABLE ONLY info_panel_ranked_option
  ADD CONSTRAINT info_panel_ranked_option_pkey PRIMARY KEY (infoPanelRanked_id);

ALTER TABLE ONLY info_channel
  ADD CONSTRAINT info_channel_pkey PRIMARY KEY (infoChannel_id);
  
ALTER TABLE ONLY info_panel_message
  ADD CONSTRAINT info_panel_message_pkey PRIMARY KEY (infoPanel_id);
  
ALTER TABLE ONLY game_info_card
  ADD CONSTRAINT game_info_card_pkey PRIMARY KEY (gameCard_id);
  
ALTER TABLE ONLY league_account
  ADD CONSTRAINT league_account_pkey PRIMARY KEY (leagueAccount_id);
  
ALTER TABLE ONLY current_game_info
  ADD CONSTRAINT current_game_info_pkey PRIMARY KEY (currentGame_id);
  
ALTER TABLE ONLY last_rank
  ADD CONSTRAINT last_rank_pkey PRIMARY KEY (lastrank_id);
  
ALTER TABLE ONLY rank_history_channel
  ADD CONSTRAINT rank_history_channel_pkey PRIMARY KEY (rhchannel_id);
  
ALTER TABLE ONLY leaderboard
  ADD CONSTRAINT leaderboard_pkey PRIMARY KEY (lead_id);
  
ALTER TABLE player 
  ADD CONSTRAINT player_fk_server_const 
  FOREIGN KEY (player_fk_server) REFERENCES server (serv_id);
  
ALTER TABLE player
  ADD CONSTRAINT player_fk_team_const 
  FOREIGN KEY (player_fk_team) REFERENCES team (team_id);
  
ALTER TABLE team
  ADD CONSTRAINT team_fk_server_const 
  FOREIGN KEY (team_fk_server) REFERENCES server (serv_id);
  
ALTER TABLE server_status
  ADD CONSTRAINT servStatus_fk_server_const 
  FOREIGN KEY (servStatus_fk_server) REFERENCES server (serv_id)
  ON DELETE CASCADE;
  
ALTER TABLE server_configuration
  ADD CONSTRAINT servConfig_fk_server_const 
  FOREIGN KEY (servConfig_fk_server) REFERENCES server (serv_id)
  ON DELETE CASCADE;
  
ALTER TABLE self_adding_option
  ADD CONSTRAINT self_adding_option_fk_serverConfig_const 
  FOREIGN KEY (selfOption_fk_serverConfig) REFERENCES server_configuration (servConfig_id)
  ON DELETE CASCADE;
  
ALTER TABLE region_option
  ADD CONSTRAINT region_option_fk_serverConfig_const 
  FOREIGN KEY (regionOption_fk_serverConfig) REFERENCES server_configuration (servConfig_id)
  ON DELETE CASCADE;
  
ALTER TABLE clean_channel_option
  ADD CONSTRAINT clean_channel_option_fk_serverConfig_const 
  FOREIGN KEY (cleanOption_fk_serverConfig) REFERENCES server_configuration (servConfig_id)
  ON DELETE CASCADE;
  
ALTER TABLE game_info_card_option
  ADD CONSTRAINT game_info_card_option_fk_serverConfig_const 
  FOREIGN KEY (gameCardOption_fk_serverConfig) REFERENCES server_configuration (servConfig_id)
  ON DELETE CASCADE;
  
ALTER TABLE role_option
  ADD CONSTRAINT role_option_fk_serverConfig_const 
  FOREIGN KEY (roleOption_fk_serverConfig) REFERENCES server_configuration (servConfig_id)
  ON DELETE CASCADE;
  
ALTER TABLE info_panel_ranked_option
  ADD CONSTRAINT info_panel_ranked_option_fk_serverConfig_const
  FOREIGN KEY (infoPanelRanked_fk_serverConfig) REFERENCES server_configuration (servConfig_id)
  ON DELETE CASCADE;
  
ALTER TABLE info_channel
  ADD CONSTRAINT info_channel_fk_server_const 
  FOREIGN KEY (infoChannel_fk_server) REFERENCES server (serv_id);
  
ALTER TABLE info_panel_message
  ADD CONSTRAINT info_panel_message_fk_infoChannel_const 
  FOREIGN KEY (infoPanel_fk_infoChannel) REFERENCES info_channel (infoChannel_id);
  
ALTER TABLE game_info_card
  ADD CONSTRAINT game_info_card_fk_infoChannel_const 
  FOREIGN KEY (gameCard_fk_infoChannel) REFERENCES info_channel (infoChannel_id);
  
ALTER TABLE league_account
  ADD CONSTRAINT league_account_fk_gameCard_const 
  FOREIGN KEY (leagueAccount_fk_gameCard) REFERENCES game_info_card (gameCard_id);
  
ALTER TABLE league_account
  ADD CONSTRAINT league_account_fk_player_const 
  FOREIGN KEY (leagueAccount_fk_player) REFERENCES player (player_id);

ALTER TABLE league_account
  ADD CONSTRAINT league_account_fk_currentGame_const 
  FOREIGN KEY (leagueAccount_fk_currentGame) REFERENCES current_game_info (currentGame_id);
  
ALTER TABLE game_info_card
  ADD CONSTRAINT game_info_card_fk_currentGame_const 
  FOREIGN KEY (gameCard_fk_currentGame) REFERENCES current_game_info (currentGame_id);
  
ALTER TABLE rank_history_channel
  ADD CONSTRAINT rank_history_channel_fk_server_const 
  FOREIGN KEY (rhChannel_fk_server) REFERENCES server (serv_id);
  
ALTER TABLE last_rank
  ADD CONSTRAINT last_rank_fk_leagueAccount_const 
  FOREIGN KEY (lastRank_fk_leagueAccount) REFERENCES league_account (leagueAccount_id);

ALTER TABLE leaderboard
  ADD CONSTRAINT leaderboard_fk_server_const
  FOREIGN KEY (lead_fk_server) REFERENCES server (serv_id);
	
CREATE INDEX idx_server_guildid 
  ON server(serv_guildId);
  
ALTER TABLE ONLY match_cache
  ADD CONSTRAINT match_cache_pkey PRIMARY KEY (mCatch_id);

ALTER TABLE match_cache
  ADD UNIQUE (mCatch_gameId, mCatch_platform);

CREATE INDEX idx_match_cache_gameId 
  ON match_cache(mCatch_gameId);

CREATE INDEX idx_match_cache_platform 
  ON match_cache(mCatch_platform);
  
ALTER TABLE ONLY banned_account
  ADD CONSTRAINT banned_account_pkey PRIMARY KEY (banAcc_id);