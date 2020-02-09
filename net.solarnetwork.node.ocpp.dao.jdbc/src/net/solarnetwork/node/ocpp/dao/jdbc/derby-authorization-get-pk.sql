SELECT
	id,created,enabled,expires,parent_id
FROM solarnode.ocpp_authorization
WHERE id = ?
