UPDATE solarnode.ocpp_authorization
SET 
	token = ?
	, enabled = ?
	, expires = ?
	, parent_id = ?
WHERE id = ?