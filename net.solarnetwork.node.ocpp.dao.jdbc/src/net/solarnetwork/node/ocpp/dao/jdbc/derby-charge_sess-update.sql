UPDATE solarnode.ocpp_charge_sess
SET 
	ended = ?
	,posted = ?
WHERE id_hi = ? AND id_lo = ?
