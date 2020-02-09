SELECT
	id_hi,id_lo,created,auth_id,conn_id,tx_id,ended,posted
FROM solarnode.ocpp_charge_sess
WHERE id_hi = ? AND id_lo = ?
