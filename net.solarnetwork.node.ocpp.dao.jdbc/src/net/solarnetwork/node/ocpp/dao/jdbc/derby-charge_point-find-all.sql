SELECT
	id,created,enabled,reg_status,ident,vendor
	,model,serial_num,box_serial_num,fw_vers,iccid
	,imsi,meter_type,meter_serial_num,conn_count
FROM solarnode.ocpp_charge_point
ORDER BY created, id