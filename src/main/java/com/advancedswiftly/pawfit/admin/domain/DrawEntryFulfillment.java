package com.advancedswiftly.pawfit.admin.domain;

import com.advancedswiftly.pawfit.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 당첨·배송 처리에서 운영자가 남기는 값. 추첨 응모(draw_entry) 한 건에 하나씩 둔다. */
@Entity
@Table(name = "draw_entry_fulfillment")
public class DrawEntryFulfillment extends BaseEntity {

    @Id
    @Column(name = "draw_entry_id")
    private Long drawEntryId;

    @Column(name = "address_confirmed", nullable = false)
    private boolean addressConfirmed;

    @Column(name = "tracking_number", length = 50)
    private String trackingNumber;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "updated_by")
    private Long updatedBy;

    protected DrawEntryFulfillment() {
    }

    public DrawEntryFulfillment(Long drawEntryId) {
        this.drawEntryId = drawEntryId;
    }

    public void confirmAddress() {
        this.addressConfirmed = true;
    }

    public void ship(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public void update(String note, Long updatedBy) {
        if (note != null) {
            this.note = note;
        }
        this.updatedBy = updatedBy;
    }

    public Long getDrawEntryId() {
        return drawEntryId;
    }

    public boolean isAddressConfirmed() {
        return addressConfirmed;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public String getNote() {
        return note;
    }
}
