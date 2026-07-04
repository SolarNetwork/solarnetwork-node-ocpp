UPDATE solarnode.ocpp_charge_point_conn
SET 
	status = ?
	,error_code = ?
	,ts = ?
	,info = ?
	,vendor_id = ?
	,vendor_error = ?
WHERE cp_id = ? AND conn_id = ?