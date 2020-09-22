ALTER TABLE current_game_info
	ADD currentgame_server VARCHAR NOT NULL;

ALTER TABLE current_game_info
	ADD currentgame_gameid VARCHAR NOT NULL;

ALTER TABLE current_game_info
	ADD UNIQUE (currentgame_server, currentgame_gameid);
	
ALTER TABLE last_rank
	ADD lastRank_tftLastTreatedMatchId VARCHAR;