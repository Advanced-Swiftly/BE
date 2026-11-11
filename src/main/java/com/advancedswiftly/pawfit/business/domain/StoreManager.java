package com.advancedswiftly.pawfit.business.domain;

import com.advancedswiftly.pawfit.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 업체와 업체 관리자의 연결. 업체당 한 명이다. */
@Entity
@Table(name = "store_manager")
public class StoreManager extends BaseEntity {

    @Id
    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    protected StoreManager() {
    }

    public StoreManager(Long storeId, Long userId) {
        this.storeId = storeId;
        this.userId = userId;
    }

    public void changeUser(Long userId) {
        this.userId = userId;
    }

    public Long getStoreId() {
        return storeId;
    }

    public Long getUserId() {
        return userId;
    }
}
