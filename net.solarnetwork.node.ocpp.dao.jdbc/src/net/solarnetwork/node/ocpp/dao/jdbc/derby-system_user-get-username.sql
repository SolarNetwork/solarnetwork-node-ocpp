SELECT
	su.id, su.created, su.username, su.password
	, cp.user_id, cp.cp_id
FROM solarnode.ocpp_system_user su
LEFT OUTER JOIN solarnode.ocpp_system_user_cp cp 
	ON su.id = cp.user_id
WHERE su.username = ?
ORDER BY cp.idx