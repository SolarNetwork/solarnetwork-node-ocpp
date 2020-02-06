UPDATE solarnode.ocpp_charge_point
SET 
	enabled = ?
	,reg_status = ?
	,vendor = ?
	,model = ?
	,serial_num = ?
	,fw_vers = ?
	,iccid = ?
	,imsi = ?
	,meter_type = ?
	,meter_serial_num = ?
WHERE id = ?