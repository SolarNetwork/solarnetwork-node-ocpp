INSERT INTO solarnode.ocpp_charge_sess
	(id_hi,id_lo,created,auth_id,cp_id,conn_id,tx_id
	,ended,end_reason,end_auth_id,posted)
VALUES 
	(?,?,?,?,?,?,COALESCE(?, NEXT VALUE FOR solarnode.ocpp_charge_tx_seq)
	,?,?,?,?)