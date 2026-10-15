package com.viller.gespatiens.access.users.domain;

import java.util.UUID;

public class User {
    private final UUID id;
    private String name;
    private String email;
    private String phoneNumber;
    private final Speciality speciality;
    // Solo guardamos el hash, nunca la contraseña en claro
    private String passwordHash;

    // Constructor para reconstruir un usuario ya existente (p.ej. al leerlo de la BD)
    private User(UUID id, String name, String email, String phoneNumber, Speciality speciality, String passwordHash) {
        if (id == null) {
            throw new IllegalArgumentException("El id no puede ser null");
        }
        validateFields(name, phoneNumber, speciality);
        this.id = id;
        this.name = name;
        this.email = validateEmail(email);
        this.phoneNumber = phoneNumber;
        this.speciality = speciality;
        this.passwordHash = validatePasswordHash(passwordHash);
    }

    // Crear un usuario nuevo: el dominio genera el id
    public static User create(String name, String email, String phoneNumber, Speciality speciality, String passwordHash) {
        return new User(UUID.randomUUID(), name, email, phoneNumber, speciality, passwordHash);
    }

    // Reconstruir un usuario que ya existía (leído de BD)
    public static User reconstitute(UUID id, String name, String email, String phoneNumber, Speciality speciality, String passwordHash) {
        return new User(id, name, email, phoneNumber, speciality, passwordHash);
    }

    // Método auxiliar compartido, para no repetir los tres "if" en ambos constructores
    private void validateFields(String name, String phoneNumber, Speciality speciality) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException("El número de teléfono no puede estar vacío");
        }
        if (speciality == null) {
            throw new IllegalArgumentException("El campo de especialidad no puede estar vacío");
        }
    }

    private String validateEmail(String email) {
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Email inválido");
        }
        return email;
    }

    private boolean isValidEmail(String email) {
        return email != null
                && !email.isBlank()
                && email.matches("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");
    }

    private String validatePasswordHash(String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía");
        }
        return passwordHash;
    }

    public void changeEmail(String newEmail) {
        this.email = validateEmail(newEmail);
    }

    public void changeName(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }
        this.name = newName;
    }
    public void changePhoneNumber(String newPhoneNumber) {
        if (newPhoneNumber == null || newPhoneNumber.isBlank()) {
            throw new IllegalArgumentException("El número de teléfono no puede estar vacío");
        }
        this.phoneNumber = newPhoneNumber;
    }

    public void changePasswordHash(String newPasswordHash) {
        this.passwordHash = validatePasswordHash(newPasswordHash);
    }

    public boolean isAdministrator() {
        return speciality == Speciality.ADMINISTRATOR;
    }


    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public Speciality getSpeciality() {
        return speciality;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
