package com.viller.gespatiens.access.users.infrastructure;

import com.viller.gespatiens.access.users.domain.Language;
import com.viller.gespatiens.access.users.domain.Speciality;
import com.viller.gespatiens.access.users.domain.Theme;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "users")
public class UserJpaEntity {

    // El id lo genera el dominio (UUID), no la base de datos: sin @GeneratedValue
    @Id
    private UUID id;

    @Column(nullable = false, length = 50)
    private String name;

    // Último cinturón de seguridad: aunque el caso de uso ya lo comprueba, la BD garantiza la unicidad
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String phoneNumber;

    // STRING guarda "ADMINISTRATOR" en vez del ordinal, así reordenar el enum no corrompe datos
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Speciality speciality;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    private Language language;

    @Column(nullable = false)
    private boolean tutorialSeen;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 6)
    private Theme theme;

    // NULL mientras el empleado invitado no haya creado su contraseña
    private String passwordHash;

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

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public Theme getTheme() {
        return theme;
    }

    public void setTheme(Theme theme) {
        this.theme = theme;
    }

    public boolean isTutorialSeen() {
        return tutorialSeen;
    }

    public void setTutorialSeen(boolean tutorialSeen) {
        this.tutorialSeen = tutorialSeen;
    }

    public Speciality getSpeciality() {
        return speciality;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setName(String newName) {
        this.name = newName;
    }

    public void setEmail(String newEmail) {
        this.email = newEmail;
    }

    public void setPhoneNumber(String newPhoneNumber) {
        this.phoneNumber = newPhoneNumber;
    }

    public void setSpeciality(Speciality newSpeciality) {
        this.speciality = newSpeciality;
    }

    public void setPasswordHash(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }
}
