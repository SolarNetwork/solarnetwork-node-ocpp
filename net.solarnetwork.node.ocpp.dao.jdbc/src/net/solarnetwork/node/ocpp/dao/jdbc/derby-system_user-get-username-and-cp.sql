SELECT
	su.id, su.created, su.username, su.password
	, cp.user_id, cp.cp_id
FROM solarnode.ocpp_system_user su
LEFT OUTER JOIN solarnode.ocpp_system_user_cp cp ON cp.user_id = su.id
WHERE su.username = ?
AND (EXISTS (
		SELECT *
		FROM solarnode.ocpp_system_user su
		INNER JOIN solarnode.ocpp_system_user_cp cp ON cp.user_id = su.id
		WHERE su.username = ? AND cp.cp_id = ?
	) OR NOT EXISTS (
		SELECT *
		FROM solarnode.ocpp_system_user su
		INNER JOIN solarnode.ocpp_system_user_cp cp ON cp.user_id = su.id
		WHERE su.username = ?		
	)
)
ORDER BY cp.idx