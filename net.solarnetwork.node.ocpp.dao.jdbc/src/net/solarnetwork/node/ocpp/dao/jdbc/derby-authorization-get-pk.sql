SELECT
	id,created,enabled,status,expires,parent_id
FROM solarnode.ocpp_authorization
WHERE id = ?
