CREATE TABLE solarnode.ocpp_charge_point_conn (
	cp_id				BIGINT NOT NULL,
	conn_id				INTEGER NOT NULL,
	created				TIMESTAMP NOT NULL WITH DEFAULT CURRENT_TIMESTAMP,
	status				SMALLINT NOT NULL WITH DEFAULT 0,
	error_code			SMALLINT NOT NULL WITH DEFAULT 0,
	ts					TIMESTAMP NOT NULL WITH DEFAULT CURRENT_TIMESTAMP,
	info				VARCHAR(50),
	vendor_id			VARCHAR(255),
	vendor_error		VARCHAR(50),
	CONSTRAINT ocpp_charge_point_conn_pk PRIMARY KEY (cp_id, conn_id),
	CONSTRAINT ocpp_charge_point_conn_charge_point_fk FOREIGN KEY (cp_id)
		REFERENCES solarnode.ocpp_charge_point (id)
		ON DELETE CASCADE
);

INSERT INTO solarnode.sn_settings (skey, svalue) 
VALUES ('solarnode.ocpp_charge_point_conn.version', '1');
