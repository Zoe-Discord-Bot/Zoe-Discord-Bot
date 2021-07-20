CREATE TABLE rank_role_option (
  rankRoleOption_id 							SERIAL				NOT NULL,
  rankRoleOption_fk_serverConfig				BIGINT				NOT NULL,
  rankRoleOption_ironId							BIGINT,
  rankRoleOption_bronzeId						BIGINT,
  rankRoleOption_silverId						BIGINT,
  rankRoleOption_goldId							BIGINT,
  rankRoleOption_platinumId						BIGINT,
  rankRoleOption_diamondId						BIGINT,
  rankRoleOption_masterId						BIGINT,
  rankRoleOption_grandMasterId					BIGINT,
  rankRoleOption_challengerId					BIGINT,
  rankRoleOption_soloqEnable					boolean,
  rankRoleOption_flexEnable						boolean,
  rankRoleOption_tftEnable						boolean
);

ALTER TABLE ONLY rank_role_option
  ADD CONSTRAINT rank_role_option_pkey PRIMARY KEY (rankRoleOption_id);
  
ALTER TABLE rank_role_option
  ADD CONSTRAINT rank_role_option_fk_serverConfig_const 
  FOREIGN KEY (rankRoleOption_fk_serverConfig) REFERENCES server_configuration (servConfig_id)
  ON DELETE CASCADE;