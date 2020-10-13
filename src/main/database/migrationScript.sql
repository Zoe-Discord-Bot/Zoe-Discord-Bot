ALTER TABLE current_game_info
	ADD currentgame_server VARCHAR;

ALTER TABLE current_game_info
	ADD currentgame_gameid VARCHAR;
	
ALTER TABLE last_rank
	ADD lastRank_tftLastTreatedMatchId VARCHAR;

ALTER TABLE league_account
	ADD leagueAccount_tftSummonerId VARCHAR,
	ADD leagueAccount_tftAccountId VARCHAR,
	ADD leagueAccount_tftPuuid VARCHAR;
	
--! AFTER TFT MIGRATION SCRIPT
	
ALTER TABLE league_account 
	ALTER COLUMN leagueAccount_tftSummonerId SET NOT NULL,
	ALTER COLUMN leagueAccount_tftAccountId SET NOT NULL,
	ALTER COLUMN leagueAccount_tftPuuid SET NOT NULL;
	
ALTER TABLE current_game_info
	ALTER COLUMN currentgame_server SET NOT NULL,
	ALTER COLUMN currentgame_gameid SET NOT NULL;