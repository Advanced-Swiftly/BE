package com.advancedswiftly.pawfit.business.domain;

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
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnDefault;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "store")
public class Store extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_id")
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    /** 업체가 속한 지역. 사업자 인증 요청 시점에는 비어 있고, 이후 운영자 심사 등에서 채워진다. */
    @Column(name = "region_id")
    private Long regionId;

    @Column(name = "description", length = 1000)
    private String description;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "store_amenity", joinColumns = @JoinColumn(name = "store_id"))
    @Column(name = "amenity_code", nullable = false, length = 50)
    private Set<String> amenities = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private StoreVerificationStatus verificationStatus;

    @Enumerated(EnumType.STRING)
    @ColumnDefault("'VISIBLE'")
    @Column(name = "visibility", nullable = false, length = 10)
    private StoreVisibility visibility = StoreVisibility.VISIBLE;

    protected Store() {
    }

    private Store(
            String name,
            String phone,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            StoreVerificationStatus verificationStatus) {
        this.name = name;
        this.phone = phone;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.verificationStatus = verificationStatus;
    }

    /** 사업자 인증을 요청하면서 등록되는 매장. 승인 전까지 PENDING 상태다. */
    public static Store registerPending(
            String name, String phone, String address, BigDecimal latitude, BigDecimal longitude) {
        return new Store(name, phone, address, latitude, longitude, StoreVerificationStatus.PENDING);
    }

    /** 운영자가 직접 등록하는 업체. 지역이 정해져 있고 확인 전까지 숨김 상태다. */
    public static Store registerByOperator(
            String name, String phone, String address, BigDecimal latitude, BigDecimal longitude, Long regionId) {
        Store store = new Store(name, phone, address, latitude, longitude, StoreVerificationStatus.UNVERIFIED);
        store.regionId = regionId;
        store.visibility = StoreVisibility.HIDDEN;
        return store;
    }

    /** 운영자 화면에서 바꾸는 기본 정보. 값이 전달된 항목만 수정한다. */
    public void updateByOperator(String name, String address, String description) {
        if (name != null) {
            this.name = name;
        }
        if (address != null) {
            this.address = address;
        }
        if (description != null) {
            this.description = description;
        }
    }

    public void changeVisibility(StoreVisibility visibility) {
        this.visibility = visibility;
    }

    public void changeVerificationStatus(StoreVerificationStatus status) {
        this.verificationStatus = status;
    }

    /** 보완 제출 시 값이 전달된 항목만 수정한다. */
    public void update(
            String name, String phone, String address, BigDecimal latitude, BigDecimal longitude) {
        if (name != null) {
            this.name = name;
        }
        if (phone != null) {
            this.phone = phone;
        }
        if (address != null) {
            this.address = address;
        }
        if (latitude != null) {
            this.latitude = latitude;
        }
        if (longitude != null) {
            this.longitude = longitude;
        }
    }

    /** 업체 운영 화면에서 즉시 수정할 수 있는 정보. 값이 전달된 항목만 수정한다. */
    public void updateProfile(String name, String phone, String description) {
        if (name != null) {
            this.name = name;
        }
        if (phone != null) {
            this.phone = phone;
        }
        if (description != null) {
            this.description = description;
        }
    }

    public void replaceAmenities(Collection<String> codes) {
        this.amenities.clear();
        this.amenities.addAll(codes);
    }

    public Long getId() {
        return id;
    }

    public Long getRegionId() {
        return regionId;
    }

    public String getDescription() {
        return description;
    }

    public Set<String> getAmenities() {
        return Collections.unmodifiableSet(amenities);
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public StoreVisibility getVisibility() {
        return visibility == null ? StoreVisibility.VISIBLE : visibility;
    }

    public StoreVerificationStatus getVerificationStatus() {
        return verificationStatus;
    }
}
