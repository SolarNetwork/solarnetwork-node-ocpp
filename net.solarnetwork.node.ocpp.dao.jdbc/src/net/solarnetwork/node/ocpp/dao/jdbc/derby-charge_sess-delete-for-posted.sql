DELETE FROM solarnode.ocpp_charge_sess
WHERE posted IS NOT NULL AND posted < ?