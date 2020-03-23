SELECT
	pr.id_hi, pr.id_lo, pr.created, pr.purpose, pr.kind
	, pr.recurrency, pr.valid_from, pr.valid_to, pr.duration, pr.start_at
	, pr.rate_unit, pr.min_rate, pe.prof_id_hi, pe.prof_id_lo, pe.start_period
	, pe.rate_limit, pe.num_phases
FROM solarnode.ocpp_charge_prof pr
LEFT OUTER JOIN solarnode.ocpp_charge_prof_period pe 
	ON pe.prof_id_hi = pr.id_hi AND pe.prof_id_lo = pr.id_lo
WHERE pr.id_hi = ? AND pr.id_lo = ?
ORDER BY pe.idx