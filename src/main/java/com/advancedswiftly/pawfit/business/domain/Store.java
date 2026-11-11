package com.advancedswiftly.pawfit.business.domain;

import com.advancedswiftly.pawfit.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private StoreVerificationStatus verificationStatus;

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

    public Long getId() {
        return id;
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

    public StoreVerificationStatus getVerificationStatus() {
        return verificationStatus;
    }
}
