package com.advancedswiftly.pawfit.publicdata;

import jakarta.persistence.*;

@Entity
@Table(name = "public_facility_image")
public class PublicFacilityImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long id;

    @Column(name = "facility_id", nullable = false)
    private Long facilityId;

    @Column(name = "image_url", nullable = false, length = 2000)
    private String imageUrl;

    protected PublicFacilityImage() {}
}
