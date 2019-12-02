-- Init Zoe DB

CREATE DATABASE zoe;
create user zoeAdmin with encrypted password 'xf345sD2#a@v'; --Random test password
grant all privileges on database zoe to zoeAdmin;

CREATE TABLE server (
  serv_id 										BIGINT 				NOT NULL,
  serv_guildId 									BIGINT 				NOT NULL,
  serv_language 								VARCHAR 			NOT NULL,
  serv_lastRefresh timestamp
);

CREATE TABLE player (
  player_id 									BIGINT 				NOT NULL,
  player_fk_server 								BIGINT 				NOT NULL,
  player_fk_team 								BIGINT,
  player_discordId 								BIGINT				NOT NULL,
  player_mentionnable 							boolean
);

CREATE TABLE team (
  team_id 										BIGINT 				NOT NULL,
  team_fk_server 								BIGINT 				NOT NULL,
  team_name 									VARCHAR 			NOT NULL
);

CREATE TABLE server_configuration (
  servConfig_id 								BIGINT 				NOT NULL,
  servConfig_fk_server 							BIGINT 				NOT NULL
);

CREATE TABLE self_adding_option (
  selfOption_id 								BIGINT 				NOT NULL,
  selfOption_fk_serverConfig 					BIGINT 				NOT NULL,
  selfOption_activate 							boolean 			DEFAULT TRUE
);

CREATE TABLE region_option (
  regionOption_id 								BIGINT 				NOT NULL,
  regionOption_fk_serverConfig 					BIGINT 				NOT NULL,
  regionOption_region 							VARCHAR
);

CREATE TABLE clean_channel_option (
  cleanOption_id								BIGINT				NOT NULL,
  cleanOption_fk_serverConfig					BIGINT				NOT NULL,
  cleanOption_channelId							BIGINT,
  cleanOption_option							VARCHAR
);

CREATE TABLE game_info_card_option (
  gameCardOption_id								BIGINT				NOT NULL,
  gameCardOption_fk_serverConfig				BIGINT				NOT NULL,
  gameCardOption_activate						boolean
);

CREATE TABLE role_option (
  roleOption_id									BIGINT				NOT NULL,
  roleOption_fk_serverConfig					BIGINT				NOT NULL,
  roleOption_roleId								BIGINT
);

CREATE TABLE info_channel (
  infoChannel_id								BIGINT				NOT NULL,
  infoChannel_fk_server							BIGINT				NOT NULL,
  infoChannel_channelId							BIGINT
);

CREATE TABLE info_panel_message (
  infoPanel_id									BIGINT				NOT NULL,
  infoPanel_fk_infoChannel						BIGINT				NOT NULL,
  infoPanel_messageId							BIGINT				NOT NULL
);

CREATE TABLE game_info_card (
  gameCard_id									BIGINT				NOT NULL,
  gameCard_fk_infoChannel						BIGINT				NOT NULL,
  gameCard_titleMessageId						BIGINT				NOT NULL,
  gameCard_infoCardMessageId					BIGINT				NOT NULL,
  gameCard_creationTime							timestamp
);

CREATE TABLE league_account (
  leagueAccount_id								BIGINT				NOT NULL,
  leagueAccount_fk_player						BIGINT				NOT NULL,
  leagueAccount_fk_gameCard						BIGINT,
  leagueAccount_summonerId						BIGINT				NOT NULL,
  leagueAccount_server							VARCHAR				NOT NULL,
  leagueAccount_currentGame						JSON
);

-- Constraints
ALTER TABLE ONLY server
  ADD CONSTRAINT server_pkey PRIMARY KEY (serv_id);
  
ALTER TABLE ONLY player 
  ADD CONSTRAINT player_pkey PRIMARY KEY (player_id);
  
ALTER TABLE ONLY team 
  ADD CONSTRAINT team_pkey PRIMARY KEY (team_id);
  
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

ALTER TABLE ONLY info_channel
  ADD CONSTRAINT info_channel_pkey PRIMARY KEY (infoChannel_id);
  
ALTER TABLE ONLY info_panel_message
  ADD CONSTRAINT info_panel_message_pkey PRIMARY KEY (infoPanel_id);
  
ALTER TABLE ONLY game_info_card
  ADD CONSTRAINT game_info_card_pkey PRIMARY KEY (gameCard_id);
  
ALTER TABLE ONLY league_account
  ADD CONSTRAINT league_account_pkey PRIMARY KEY (leagueAccount_id);
  
ALTER TABLE player 
  ADD CONSTRAINT player_fk_server_const 
  FOREIGN KEY (player_fk_server) REFERENCES server (serv_id);
  
ALTER TABLE player
  ADD CONSTRAINT player_fk_team_const 
  FOREIGN KEY (player_fk_team) REFERENCES team (team_id);
  
ALTER TABLE team
  ADD CONSTRAINT team_fk_server_const 
  FOREIGN KEY (team_fk_server) REFERENCES server (serv_id);
  
ALTER TABLE server_configuration
  ADD CONSTRAINT servConfig_fk_server_const 
  FOREIGN KEY (servConfig_fk_server) REFERENCES server (serv_id);
  
ALTER TABLE self_adding_option
  ADD CONSTRAINT self_adding_option_fk_serverConfig_const 
  FOREIGN KEY (selfOption_fk_serverConfig) REFERENCES server_configuration (servConfig_id);
  
ALTER TABLE region_option
  ADD CONSTRAINT region_option_fk_serverConfig_const 
  FOREIGN KEY (regionOption_fk_serverConfig) REFERENCES server_configuration (servConfig_id);
  
ALTER TABLE clean_channel_option
  ADD CONSTRAINT clean_channel_option_fk_serverConfig_const 
  FOREIGN KEY (cleanOption_fk_serverConfig) REFERENCES server_configuration (servConfig_id);
  
ALTER TABLE game_info_card_option
  ADD CONSTRAINT game_info_card_option_fk_serverConfig_const 
  FOREIGN KEY (gameCardOption_fk_serverConfig) REFERENCES server_configuration (servConfig_id);
  
ALTER TABLE role_option
  ADD CONSTRAINT role_option_fk_serverConfig_const 
  FOREIGN KEY (roleOption_fk_serverConfig) REFERENCES server_configuration (servConfig_id);
  
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
