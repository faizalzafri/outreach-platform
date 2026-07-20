package com.outreach.platform.event.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

/**
 * Junction entity for the many-to-many relationship between events and beneficiaries.
 */
@Entity
@Table(name = "event_beneficiary")
public class EventBeneficiaryEntity {

    @EmbeddedId
    private EventBeneficiaryId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("eventId")
    @JoinColumn(name = "event_id")
    private EventEntity event;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("beneficiaryId")
    @JoinColumn(name = "beneficiary_id")
    private BeneficiaryEntity beneficiary;

    public EventBeneficiaryEntity() {
    }

    public EventBeneficiaryEntity(EventEntity event, BeneficiaryEntity beneficiary) {
        this.event = event;
        this.beneficiary = beneficiary;
        this.id = new EventBeneficiaryId(event.getId(), beneficiary.getId());
    }

    public EventBeneficiaryId getId() {
        return id;
    }

    public void setId(EventBeneficiaryId id) {
        this.id = id;
    }

    public EventEntity getEvent() {
        return event;
    }

    public void setEvent(EventEntity event) {
        this.event = event;
    }

    public BeneficiaryEntity getBeneficiary() {
        return beneficiary;
    }

    public void setBeneficiary(BeneficiaryEntity beneficiary) {
        this.beneficiary = beneficiary;
    }
}
