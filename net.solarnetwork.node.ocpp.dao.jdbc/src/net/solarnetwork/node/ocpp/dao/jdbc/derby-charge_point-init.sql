CREATE TABLE solarnode.ocpp_charge_point (
	id					VARCHAR(255) NOT NULL,
	created				TIMESTAMP NOT NULL WITH DEFAULT CURRENT_TIMESTAMP,
	enabled				BOOLEAN NOT NULL DEFAULT true,
	reg_status			SMALLINT NOT NULL DEFAULT 0,
	vendor				VARCHAR(20) NOT NULL,
	model				VARCHAR(20) NOT NULL,
	serial_num			VARCHAR(25),
	box_serial_num		VARCHAR(25),
	fw_vers				VARCHAR(50),
	iccid				VARCHAR(20),
	imsi				VARCHAR(20),
	meter_type			VARCHAR(25),
	meter_serial_num	VARCHAR(25),
	conn_count			SMALLINT NOT NULL DEFAULT 0,
	CONSTRAINT ocpp_charge_point_pk PRIMARY KEY (id)
);

INSERT INTO solarnode.sn_settings (skey, svalue) 
VALUES ('solarnode.ocpp_charge_point.version', '1');
