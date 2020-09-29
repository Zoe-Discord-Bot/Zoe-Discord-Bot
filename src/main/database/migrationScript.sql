ALTER TABLE current_game_info
	ADD currentgame_server VARCHAR NOT NULL;

ALTER TABLE current_game_info
	ADD currentgame_gameid VARCHAR NOT NULL;

ALTER TABLE current_game_info
	ADD UNIQUE (currentgame_server, currentgame_gameid);
	
ALTER TABLE last_rank
	ADD lastRank_tftLastTreatedMatchId VARCHAR;

ALTER TABLE league_account
	ADD leagueAccount_tftSummonerId VARCHAR,
	ADD leagueAccount_tftAccountId VARCHAR,
	ADD leagueAccount_tftPuuid VARCHAR;
	
--! AFTER TFT MIGRATION SCRIPT
	
ALTER TABLE league_account 
	ADD CONSTRAINT leagueAccount_tftSummonerId NOT NULL,
	ADD CONSTRAINT leagueAccount_tftAccountId NOT NULL,
	ADD CONSTRAINT leagueAccount_tftPuuid NOT NULL;