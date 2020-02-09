CREATE TABLE solarnode.ocpp_authorization (
	id					VARCHAR(20) NOT NULL,
	created				TIMESTAMP NOT NULL WITH DEFAULT CURRENT_TIMESTAMP,
	enabled				BOOLEAN NOT NULL DEFAULT true,
	expires				TIMESTAMP,
	parent_id			VARCHAR(20),
	CONSTRAINT ocpp_authorization_pk PRIMARY KEY (id)
);

INSERT INTO solarnode.sn_settings (skey, svalue) 
VALUES ('solarnode.ocpp_authorization.version', '1');
