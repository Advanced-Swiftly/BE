package com.advancedswiftly.pawfit.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "breed")
public class Breed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "breed_id")
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected Breed() {
    }

    private Breed(String name) {
        this.name = name;
        this.active = true;
    }

    public static Breed create(String name) {
        return new Breed(name);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
