CREATE SEQUENCE solarnode.ocpp_charge_tx_seq
AS INT START WITH 1 INCREMENT BY 1 CYCLE;

CREATE TABLE solarnode.ocpp_charge_sess (
	id_hi				BIGINT NOT NULL,
	id_lo				BIGINT NOT NULL,
	created				TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	auth_id				VARCHAR(20) NOT NULL,
	cp_id				BIGINT NOT NULL,
	conn_id				INTEGER NOT NULL,
	tx_id				INTEGER NOT NULL,
	ended				TIMESTAMP,
	end_reason			SMALLINT NOT NULL DEFAULT 0,
	end_auth_id			VARCHAR(20),
	posted				TIMESTAMP,
	CONSTRAINT ocpp_charge_sess_pk PRIMARY KEY (id_hi, id_lo),
	CONSTRAINT ocpp_charge_sess_charge_point_fk FOREIGN KEY (cp_id)
		REFERENCES solarnode.ocpp_charge_point (id)
		ON DELETE CASCADE
);

CREATE TABLE solarnode.ocpp_charge_sess_reading (
	sess_id_hi			BIGINT NOT NULL,
	sess_id_lo			BIGINT NOT NULL,
	ts					TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	location			SMALLINT NOT NULL DEFAULT 0,
	unit 				SMALLINT NOT NULL DEFAULT 0,
	context 			SMALLINT NOT NULL DEFAULT 0,
	measurand			SMALLINT NOT NULL DEFAULT 0,
	phase				SMALLINT,
	reading 			VARCHAR(64) NOT NULL,
	CONSTRAINT ocpp_charge_sess_reading_charge_sess_fk FOREIGN KEY (sess_id_hi, sess_id_lo)
		REFERENCES solarnode.ocpp_charge_sess (id_hi, id_lo)
		ON DELETE CASCADE
);

INSERT INTO solarnode.sn_settings (skey, svalue) 
VALUES ('solarnode.ocpp_charge_sess.version', '1');
