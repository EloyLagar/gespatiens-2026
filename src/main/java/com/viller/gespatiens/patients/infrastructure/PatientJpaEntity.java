package com.viller.gespatiens.patients.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "patients")
public class PatientJpaEntity {

    // El id lo genera el dominio (UUID), no la base de datos: sin @GeneratedValue
    @Id
    private UUID id;

    // unique: dos residentes activos no pueden compartir número. Los antiguos tienen null,
    // y PostgreSQL permite varios null en una columna unique
    @Column(unique = true)
    private Integer number;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 100)
    private String surname;

    @Column(nullable = false, unique = true, length = 9)
    private String dni;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(nullable = false, length = 20)
    private String phoneNumber;

    @Column(length = 25)
    private String sip;

    @Column(unique = true, length = 25)
    private String visitCode;

    @Column(columnDefinition = "text")
    private String belongings;

    @Column(length = 100)
    private String abuseSubstances;

    @Column(columnDefinition = "text")
    private String extraInfo;

    @Column(nullable = false)
    private LocalDate entryDate;

    private LocalDate exitDate;

    public UUID getId() {
        return id;
    }

    public Integer getNumber() {
        return number;
    }

    public String getName() {
        return name;
    }

    public String getSurname() {
        return surname;
    }

    public String getDni() {
        return dni;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getAddress() {
        return address;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getSip() {
        return sip;
    }

    public String getVisitCode() {
        return visitCode;
    }

    public String getBelongings() {
        return belongings;
    }

    public String getAbuseSubstances() {
        return abuseSubstances;
    }

    public String getExtraInfo() {
        return extraInfo;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public LocalDate getExitDate() {
        return exitDate;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setSip(String sip) {
        this.sip = sip;
    }

    public void setVisitCode(String visitCode) {
        this.visitCode = visitCode;
    }

    public void setBelongings(String belongings) {
        this.belongings = belongings;
    }

    public void setAbuseSubstances(String abuseSubstances) {
        this.abuseSubstances = abuseSubstances;
    }

    public void setExtraInfo(String extraInfo) {
        this.extraInfo = extraInfo;
    }

    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
    }

    public void setExitDate(LocalDate exitDate) {
        this.exitDate = exitDate;
    }
}
