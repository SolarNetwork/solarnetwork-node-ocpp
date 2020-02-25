SELECT
	id,created,token,enabled,expires,parent_id
FROM solarnode.ocpp_authorization
ORDER BY created, id