INSERT INTO solarnode.ocpp_charge_sess
	(id_hi,id_lo,created,auth_id,conn_id,tx_id,ended,posted)
VALUES 
	(?,?,?,?,?,NEXT VALUE FOR solarnode.ocpp_charge_tx_seq,?,?)