package com.viller.gespatiens.audit.domain;

// Lo que se registra: cada consulta o cambio de los datos sensibles de un residente (salud, situación judicial,
// familia). Los listados no se registran, solo abrir o tocar la información de una persona concreta
public enum AuditAction {
    VIEW_RECORD,
    ADMIT,
    EDIT_RECORD,
    DISCHARGE,
    READMIT,
    DELETE_RECORD,
    VIEW_VISITORS,
    EDIT_VISITORS,
    VIEW_REFERRAL,
    EDIT_REFERRAL,
    VIEW_REPORT,
    DOWNLOAD_REPORT,
    EDIT_REPORT,
    CLOSE_REPORT,
    REOPEN_REPORT,
    VIEW_FOLLOW_UP,
    EDIT_FOLLOW_UP,
    ASSIGN_TUTORS
}
