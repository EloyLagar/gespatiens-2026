package com.viller.gespatiens.access.users.domain;

import java.util.UUID;

public class User {
    private final UUID id;
    private String name;
    private String email;
    private String phoneNumber;
    private Speciality speciality;
    // Solo guardamos el hash, nunca la contraseña en claro. null mientras el empleado invitado
    // no haya creado la suya desde el enlace del correo
    private String passwordHash;
    // El idioma de la aplicación que ha elegido (RNF9)
    private Language language;

    // Constructor para reconstruir un usuario ya existente (p.ej. al leerlo de la BD)
    private User(UUID id, String name, String email, String phoneNumber, Speciality speciality, String passwordHash,
                 Language language) {
        if (id == null) {
            throw new IllegalArgumentException("El id no puede ser null");
        }
        validateFields(name, phoneNumber, speciality);
        this.id = id;
        this.name = name;
        this.email = validateEmail(email);
        this.phoneNumber = phoneNumber;
        this.speciality = speciality;
        this.passwordHash = passwordHash == null ? null : validatePasswordHash(passwordHash);
        this.language = language == null ? Language.ES : language;
    }

    // Crear un usuario nuevo: el dominio genera el id
    public static User create(String name, String email, String phoneNumber, Speciality speciality, String passwordHash) {
        return new User(UUID.randomUUID(), name, email, phoneNumber, speciality, passwordHash, Language.ES);
    }

    // Memoria final: la administración da de alta al empleado y este recibe un correo para crear su
    // propia contraseña. Hasta entonces no puede entrar
    public static User invite(String name, String email, String phoneNumber, Speciality speciality) {
        return new User(UUID.randomUUID(), name, email, phoneNumber, speciality, null, Language.ES);
    }

    // Reconstruir un usuario que ya existía (leído de BD)
    public static User reconstitute(UUID id, String name, String email, String phoneNumber, Speciality speciality,
                                    String passwordHash, Language language) {
        return new User(id, name, email, phoneNumber, speciality, passwordHash, language);
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

    public void changeSpeciality(Speciality newSpeciality) {
        if (newSpeciality == null) {
            throw new IllegalArgumentException("El campo de especialidad no puede estar vacío");
        }
        this.speciality = newSpeciality;
    }

    public void changePasswordHash(String newPasswordHash) {
        this.passwordHash = validatePasswordHash(newPasswordHash);
    }

    // false: está invitado y todavía no ha creado su contraseña
    public boolean hasPassword() {
        return passwordHash != null;
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

    public void changeLanguage(Language newLanguage) {
        if (newLanguage == null) {
            throw new IllegalArgumentException("Indica el idioma");
        }
        this.language = newLanguage;
    }

    public Language getLanguage() {
        return language;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
