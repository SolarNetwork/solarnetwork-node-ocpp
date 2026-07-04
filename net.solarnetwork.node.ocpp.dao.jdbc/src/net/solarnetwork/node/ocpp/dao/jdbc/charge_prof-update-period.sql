UPDATE solarnode.ocpp_charge_prof_period SET
	start_period = ?
	, rate_limit = ?
	, num_phases  = ?
WHERE prof_id_hi = ? AND prof_id_lo = ? AND idx = ?