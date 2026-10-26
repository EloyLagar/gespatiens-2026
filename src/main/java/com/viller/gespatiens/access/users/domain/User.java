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
    // Si ya ha visto (o se ha saltado) el tutorial de bienvenida
    private boolean tutorialSeen;

    // Constructor para reconstruir un usuario ya existente (p.ej. al leerlo de la BD)
    private User(UUID id, String name, String email, String phoneNumber, Speciality speciality, String passwordHash,
                 Language language, boolean tutorialSeen) {
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
        this.tutorialSeen = tutorialSeen;
    }

    // Crear un usuario nuevo: el dominio genera el id
    public static User create(String name, String email, String phoneNumber, Speciality speciality, String passwordHash) {
        return new User(UUID.randomUUID(), name, email, phoneNumber, speciality, passwordHash, Language.ES, false);
    }

    // Memoria final: la administración da de alta al empleado y este recibe un correo para crear su
    // propia contraseña. Hasta entonces no puede entrar
    public static User invite(String name, String email, String phoneNumber, Speciality speciality) {
        return new User(UUID.randomUUID(), name, email, phoneNumber, speciality, null, Language.ES, false);
    }

    // Reconstruir un usuario que ya existía (leído de BD)
    public static User reconstitute(UUID id, String name, String email, String phoneNumber, Speciality speciality,
                                    String passwordHash, Language language, boolean tutorialSeen) {
        return new User(id, name, email, phoneNumber, speciality, passwordHash, language, tutorialSeen);
    }

    // Lo que cabe en la columna users.name. Sin esta comprobación, un nombre más largo llegaba a la base de
    // datos y la API respondía 409 "conflicto" en vez de explicar el error
    public static final int MAX_NAME_LENGTH = 50;
    // Como el de los residentes. El estándar no admite direcciones de correo más largas de 254
    public static final int MAX_PHONE_LENGTH = 20;
    public static final int MAX_EMAIL_LENGTH = 254;

    // Método auxiliar compartido, para no repetir los tres "if" en ambos constructores
    private void validateFields(String name, String phoneNumber, Speciality speciality) {
        validateName(name);
        validatePhoneNumber(phoneNumber);
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
                && email.length() <= MAX_EMAIL_LENGTH
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
        validateName(newName);
        this.name = newName;
    }

    private static void validatePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException("El número de teléfono no puede estar vacío");
        }
        if (phoneNumber.length() > MAX_PHONE_LENGTH) {
            throw new IllegalArgumentException(
                    "El número de teléfono no puede tener más de " + MAX_PHONE_LENGTH + " caracteres");
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("El nombre no puede tener más de " + MAX_NAME_LENGTH + " caracteres");
        }
    }
    public void changePhoneNumber(String newPhoneNumber) {
        validatePhoneNumber(newPhoneNumber);
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

    public void markTutorialSeen() {
        this.tutorialSeen = true;
    }

    public boolean hasSeenTutorial() {
        return tutorialSeen;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
