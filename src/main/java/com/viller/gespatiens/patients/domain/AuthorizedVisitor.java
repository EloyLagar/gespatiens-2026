package com.viller.gespatiens.patients.domain;

import java.util.UUID;

// Una persona autorizada a visitar a un residente (Visitors en la memoria): "las personas que están
// autorizadas a visitarles, la relación de estas con el usuario y su número de teléfono"
public class AuthorizedVisitor {

    private final UUID id;
    private final UUID patientId;
    private String name;
    private String relationship;
    private String phoneNumber;

    private AuthorizedVisitor(UUID id, UUID patientId, String name, String relationship, String phoneNumber) {
        if (id == null || patientId == null) {
            throw new IllegalArgumentException("La persona autorizada necesita id y residente");
        }
        this.id = id;
        this.patientId = patientId;
        describe(name, relationship, phoneNumber);
    }

    public static AuthorizedVisitor authorize(UUID patientId, String name, String relationship, String phoneNumber) {
        return new AuthorizedVisitor(UUID.randomUUID(), patientId, name, relationship, phoneNumber);
    }

    public static AuthorizedVisitor reconstitute(UUID id, UUID patientId, String name, String relationship,
                                                 String phoneNumber) {
        return new AuthorizedVisitor(id, patientId, name, relationship, phoneNumber);
    }

    public void update(String newName, String newRelationship, String newPhoneNumber) {
        describe(newName, newRelationship, newPhoneNumber);
    }

    private void describe(String name, String relationship, String phoneNumber) {
        this.name = required(name, "nombre", 50);
        this.relationship = required(relationship, "parentesco", 50);
        this.phoneNumber = required(phoneNumber, "teléfono", 20);
    }

    private static String required(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Indica el " + field + " de la persona autorizada");
        }
        String trimmed = value.strip();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException("El " + field + " no puede superar los " + maxLength + " caracteres");
        }
        return trimmed;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public String getName() {
        return name;
    }

    public String getRelationship() {
        return relationship;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
}
