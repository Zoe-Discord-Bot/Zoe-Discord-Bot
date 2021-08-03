-- Force verifiation Update
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
  
-- Supporter Subscription Update
CREATE TABLE zoe_user (
  zoeUser_id										SERIAL,
  zoeUser_discordId									BIGINT				UNIQUE NOT NULL,
  zoeUser_fullMonthSupported						BIGINT				DEFAULT 0 NOT NULL,
  zoeUser_totalGiven								BIGINT				DEFAULT 0 NOT NULL
);

CREATE TABLE role (
  role_id											SERIAL,
  role_roleId										BIGINT				UNIQUE NOT NULL
);

ALTER TABLE ONLY zoe_user
  ADD CONSTRAINT zoe_user_pkey PRIMARY KEY (zoeUser_id);
  
ALTER TABLE ONLY role
  ADD CONSTRAINT role_pkey PRIMARY KEY (role_id);

CREATE TABLE zoe_user_role (
  zoeUserRole_fk_user_id							BIGINT				REFERENCES zoe_user(zoeUser_id),
  zoeUserRole_fk_role_id							BIGINT				REFERENCES role(role_id),
  zoeUserRole_endOfTheSubscription					TIMESTAMP			WITHOUT TIME ZONE,
  PRIMARY KEY (zoeUserRole_fk_user_id, zoeUserRole_fk_role_id)
);