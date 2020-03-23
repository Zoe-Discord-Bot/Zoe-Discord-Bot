ALTER TABLE last_rank
ADD COLUMN lastRank_soloqSecond json,
ADD COLUMN lastRank_soloqLastRefresh		TIMESTAMP			WITHOUT TIME ZONE,
ADD COLUMN lastRank_flexSecond json,
ADD COLUMN lastRank_flexLastRefresh			TIMESTAMP			WITHOUT TIME ZONE,
ADD COLUMN lastRank_tftSecond json,
ADD COLUMN lastRank_tftLastRefresh			TIMESTAMP			WITHOUT TIME ZONE;

CREATE TABLE info_panel_ranked_option (
  infoPanelRanked_id 								SERIAL,
  infoPanelRanked_fk_serverConfig 					BIGINT 				NOT NULL,
  infoPanelRanked_activate 							boolean 			DEFAULT TRUE
);

ALTER TABLE ONLY info_panel_ranked_option
  ADD CONSTRAINT info_panel_ranked_option_pkey PRIMARY KEY (infoPanelRanked_id);
  
ALTER TABLE info_panel_ranked_option
  ADD CONSTRAINT info_panel_ranked_option_fk_serverConfig_const
  FOREIGN KEY (infoPanelRanked_fk_serverConfig) REFERENCES server_configuration (servConfig_id)
  ON DELETE CASCADE;
  
