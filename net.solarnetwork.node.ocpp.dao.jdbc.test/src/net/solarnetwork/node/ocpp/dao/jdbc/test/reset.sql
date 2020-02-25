drop table solarnode.ocpp_charge_sess_reading;
drop table solarnode.ocpp_charge_sess;
drop sequence solarnode.ocpp_charge_tx_seq restrict;
delete from solarnode.sn_settings where skey = 'solarnode.ocpp_charge_sess.version';

drop table ocpp_charge_point_conn;
delete from solarnode.sn_settings where skey = 'solarnode.ocpp_charge_point_conn.version';

drop table solarnode.ocpp_charge_point;
delete from solarnode.sn_settings where skey = 'solarnode.ocpp_charge_point.version';
