SELECT
	id_hi,id_lo,created,auth_id,cp_id,conn_id,tx_id
	,ended,end_reason,end_auth_id,posted
FROM solarnode.ocpp_charge_sess
ORDER BY cp_id,conn_id,created