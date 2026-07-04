UPDATE solarnode.ocpp_charge_prof SET
	purpose = ?
	, kind = ?
	, recurrency = ?
	, valid_from = ?
	, valid_to = ?
	, duration = ?
	, start_at = ?
	, rate_unit = ?
	, min_rate = ?
WHERE id_hi = ? AND id_lo = ?