SELECT
	cp_id,conn_id,created,status,error_code
	,ts,info,vendor_id,vendor_error
FROM solarnode.ocpp_charge_point_conn
WHERE cp_id = ? AND conn_id = ?