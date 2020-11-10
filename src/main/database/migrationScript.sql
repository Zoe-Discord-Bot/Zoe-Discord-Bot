CREATE TABLE rank_channel_filter_option (
  rankchannelFilterOption_id								SERIAL,
  rankchannelFilterOption_fk_serverConfig					BIGINT				NOT NULL,
  rankchannelFilterOption_option							VARCHAR
);

ALTER TABLE ONLY rank_channel_filter_option
  ADD CONSTRAINT rank_channel_filter_option_pkey PRIMARY KEY (rankchannelFilterOption_id);
  
ALTER TABLE rank_channel_filter_option
  ADD CONSTRAINT rank_channel_filter_option_fk_serverConfig_const 
  FOREIGN KEY (rankchannelFilterOption_fk_serverConfig) REFERENCES server_configuration (servConfig_id)
  ON DELETE CASCADE;