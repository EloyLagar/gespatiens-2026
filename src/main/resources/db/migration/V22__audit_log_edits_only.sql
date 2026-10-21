-- El registro solo guarda cambios (decisión del centro): fuera las consultas que se anotaron hasta ahora
DELETE FROM audit_log
WHERE action IN ('VIEW_RECORD', 'VIEW_VISITORS', 'VIEW_REFERRAL', 'VIEW_REPORT', 'DOWNLOAD_REPORT', 'VIEW_FOLLOW_UP');
