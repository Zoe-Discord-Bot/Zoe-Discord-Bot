CREATE TABLE force_verification_option (
  verificationOption_id								SERIAL,
  verificationOption_fk_serverConfig				BIGINT				NOT NULL,
  verificationOption_activate						boolean				NOT NULL
);

ALTER TABLE ONLY force_verification_option
  ADD CONSTRAINT force_verification_option_pkey PRIMARY KEY (verificationOption_id);
  
ALTER TABLE force_verification_option
  ADD CONSTRAINT force_verification_option_fk_serverConfig_const 
  FOREIGN KEY (verificationOption_fk_serverConfig) REFERENCES server_configuration (servConfig_id)
  ON DELETE CASCADE;