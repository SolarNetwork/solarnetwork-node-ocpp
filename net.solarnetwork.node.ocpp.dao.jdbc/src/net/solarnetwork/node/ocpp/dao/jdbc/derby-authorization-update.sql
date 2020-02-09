UPDATE solarnode.ocpp_authorization
SET 
	enabled = ?
	,status = ?
	,expires = ?
	,parent_id = ?
WHERE id = ?