SELECT
	sess_id_hi,sess_id_lo,ts,location,unit
	,context,measurand,phase,reading
FROM solarnode.ocpp_charge_sess_reading
WHERE sess_id_hi = ? AND sess_id_lo = ?
ORDER BY ts,context,location,measurand