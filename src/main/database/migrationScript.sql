CREATE TABLE leaderboard (
  lead_id 										SERIAL,
  lead_fk_server								BIGINT				NOT NULL,
  lead_message_channelId						BIGINT 				NOT NULL,
  lead_message_id								BIGINT 				NOT NULL,
  lead_type		 								BIGINT				NOT NULL,
  lead_data										json,
  lead_lastRefresh 								TIMESTAMP			WITHOUT TIME ZONE
);

ALTER TABLE ONLY leaderboard
  ADD CONSTRAINT leaderboard_pkey PRIMARY KEY (lead_id);
  
ALTER TABLE leaderboard
  ADD CONSTRAINT leaderboard_fk_server_const
  FOREIGN KEY (lead_fk_server) REFERENCES server (serv_id);