ALTER TABLE solarnode.ocpp_charge_sess
ADD COLUMN evse_id INTEGER WITH DEFAULT 0;

UPDATE solarnode.ocpp_charge_sess
SET evse_id = 0;

ALTER TABLE solarnode.ocpp_charge_sess
ALTER COLUMN evse_id NOT NULL;

ALTER TABLE solarnode.ocpp_charge_sess
ALTER COLUMN tx_id SET DATA TYPE CHARACTER VARYING(36);

ALTER TABLE solarnode.ocpp_charge_sess
ALTER COLUMN tx_id NOT NULL;

UPDATE solarnode.sn_settings SET svalue = '2'
WHERE skey = 'solarnode.ocpp_charge_sess.version';
