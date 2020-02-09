CREATE SEQUENCE solarnode.ocpp_charge_tx_seq
AS INT START WITH 1 INCREMENT BY 1 CYCLE;

CREATE TABLE solarnode.ocpp_charge_sess (
	id_hi				BIGINT NOT NULL,
	id_lo				BIGINT NOT NULL,
	created				TIMESTAMP NOT NULL WITH DEFAULT CURRENT_TIMESTAMP,
	auth_id				VARCHAR(20) NOT NULL,
	conn_id				INTEGER NOT NULL,
	tx_id				INTEGER NOT NULL,
	ended				TIMESTAMP,
	posted				TIMESTAMP,
	CONSTRAINT ocpp_charge_sess_pk PRIMARY KEY (id_hi, id_lo)
);

CREATE TABLE solarnode.ocpp_charge_sess_value (
	sess_id_hi			BIGINT NOT NULL,
	sess_id_lo			BIGINT NOT NULL,
	ts					TIMESTAMP NOT NULL WITH DEFAULT CURRENT_TIMESTAMP,
	format              SMALLINT NOT NULL WITH DEFAULT 0,
	location			SMALLINT NOT NULL WITH DEFAULT 0,
	unit 				SMALLINT NOT NULL WITH DEFAULT 0,
	context 			SMALLINT NOT NULL WITH DEFAULT 0,
	measurand			SMALLINT NOT NULL WITH DEFAULT 0,
	phase				SMALLINT NOT NULL WITH DEFAULT 0,
	reading 			VARCHAR(64) NOT NULL,
	CONSTRAINT ocpp_charge_sess_value_charge_sess_fk FOREIGN KEY (sess_id_hi, sess_id_lo)
		REFERENCES solarnode.ocpp_charge_sess (id_hi, id_lo)
		ON DELETE CASCADE
);

INSERT INTO solarnode.sn_settings (skey, svalue) 
VALUES ('solarnode.ocpp_charge_sess.version', '1');
