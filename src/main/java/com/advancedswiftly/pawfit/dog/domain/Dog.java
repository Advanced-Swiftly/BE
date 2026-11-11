package com.advancedswiftly.pawfit.dog.domain;

import com.advancedswiftly.pawfit.auth.domain.Breed;
import com.advancedswiftly.pawfit.auth.domain.User;
import com.advancedswiftly.pawfit.global.common.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "dog")
public class Dog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dog_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "breed_id", nullable = false)
    private Breed breed;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "weight_kg", precision = 6, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "is_default", nullable = false)
    private boolean defaultDog;

    @Enumerated(EnumType.STRING)
    @Column(name = "vaccination_status", nullable = false, length = 30)
    private VaccinationStatus vaccinationStatus;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "dog_equipment", joinColumns = @JoinColumn(name = "dog_id"))
    @Column(name = "equipment_code", nullable = false, length = 50)
    private Set<String> equipmentCodes = new LinkedHashSet<>();

    protected Dog() {
    }

    private Dog(
            User user,
            Breed breed,
            String name,
            LocalDate birthDate,
            BigDecimal weightKg,
            boolean defaultDog,
            List<String> equipmentCodes) {
        this.user = user;
        this.breed = breed;
        this.name = name.trim();
        this.birthDate = birthDate;
        this.weightKg = weightKg;
        this.defaultDog = defaultDog;
        this.vaccinationStatus = VaccinationStatus.NOT_SUBMITTED;
        replaceEquipmentCodes(equipmentCodes);
    }

    public static Dog create(
            User user,
            Breed breed,
            String name,
            LocalDate birthDate,
            BigDecimal weightKg,
            boolean defaultDog,
            List<String> equipmentCodes) {
        return new Dog(user, breed, name, birthDate, weightKg, defaultDog, equipmentCodes);
    }

    public void updateName(String name) {
        this.name = name.trim();
    }

    public void updateBreed(Breed breed) {
        this.breed = breed;
    }

    public void updateWeightKg(BigDecimal weightKg) {
        this.weightKg = weightKg;
    }

    public void replaceEquipmentCodes(List<String> equipmentCodes) {
        this.equipmentCodes.clear();
        equipmentCodes.stream()
                .map(String::trim)
                .forEach(this.equipmentCodes::add);
    }

    public void designateAsDefault() {
        this.defaultDog = true;
    }

    public void clearDefault() {
        this.defaultDog = false;
    }

    public Long getId() {
        return id;
    }

    public Breed getBreed() {
        return breed;
    }

    public String getName() {
        return name;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public boolean isDefaultDog() {
        return defaultDog;
    }

    public VaccinationStatus getVaccinationStatus() {
        return vaccinationStatus;
    }

    public Set<String> getEquipmentCodes() {
        return Collections.unmodifiableSet(equipmentCodes);
    }
}
