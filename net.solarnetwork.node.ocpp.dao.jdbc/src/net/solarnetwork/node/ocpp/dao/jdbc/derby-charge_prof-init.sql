CREATE TABLE solarnode.ocpp_charge_prof (
	id_hi				BIGINT NOT NULL,
	id_lo				BIGINT NOT NULL,
	created				TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	purpose				SMALLINT NOT NULL DEFAULT 0,
	kind				SMALLINT NOT NULL DEFAULT 0,
	recurrency			SMALLINT NOT NULL DEFAULT 0,
	valid_from			TIMESTAMP,
	valid_to			TIMESTAMP,
	duration			INTEGER,
	start_at			TIMESTAMP,
	rate_unit			SMALLINT NOT NULL DEFAULT 0,
	min_rate			DECIMAL(6,1),
	CONSTRAINT ocpp_charge_prof_pk PRIMARY KEY (id_hi, id_lo)
);

CREATE TABLE solarnode.ocpp_charge_prof_period (
	prof_id_hi			BIGINT NOT NULL,
	prof_id_lo			BIGINT NOT NULL,
	idx					INTEGER NOT NULL,
	start_period		INTEGER NOT NULL,
	rate_limit			DECIMAL(10,1) NOT NULL,
	num_phases			SMALLINT,
	CONSTRAINT ocpp_charge_prof_period_pk PRIMARY KEY (prof_id_hi, prof_id_lo, idx),
	CONSTRAINT ocpp_charge_prof_period_charge_prof_fk FOREIGN KEY (prof_id_hi, prof_id_lo)
		REFERENCES solarnode.ocpp_charge_prof (id_hi, id_lo)
		ON DELETE CASCADE
);

INSERT INTO solarnode.sn_settings (skey, svalue) 
VALUES ('solarnode.ocpp_charge_prof.version', '1');
