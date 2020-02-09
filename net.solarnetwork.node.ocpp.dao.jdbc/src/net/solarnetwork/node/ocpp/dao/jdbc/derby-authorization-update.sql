UPDATE solarnode.ocpp_authorization
SET 
	enabled = ?
	,expires = ?
	,parent_id = ?
WHERE id = ?