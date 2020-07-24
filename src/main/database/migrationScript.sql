CREATE TABLE banned_account (
  banAcc_id										SERIAL,
  banAcc_summonerId								VARCHAR				NOT NULL,
  banAcc_server									VARCHAR				NOT NULL
);

ALTER TABLE ONLY banned_account
  ADD CONSTRAINT banned_account_pkey PRIMARY KEY (banAcc_id);