package com.viller.gespatiens.audit.domain;

// Lo que se registra: cada cambio de los datos sensibles de un residente (salud, situación judicial, familia).
// Las consultas no se registran (decisión del centro): solo quién ha tocado la información de una persona
public enum AuditAction {
    ADMIT,
    EDIT_RECORD,
    DISCHARGE,
    READMIT,
    DELETE_RECORD,
    EDIT_VISITORS,
    EDIT_REFERRAL,
    EDIT_REPORT,
    CLOSE_REPORT,
    REOPEN_REPORT,
    EDIT_FOLLOW_UP,
    ASSIGN_TUTORS
}
