UPDATE solarnode.ocpp_charge_sess
SET 
	ended = ?
	,end_reason = ?
	,end_auth_id = ?
	,posted = ?
WHERE id_hi = ? AND id_lo = ?