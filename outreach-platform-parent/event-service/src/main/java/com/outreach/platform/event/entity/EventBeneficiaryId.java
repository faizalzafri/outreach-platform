package com.outreach.platform.event.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Composite primary key for the event_beneficiary junction table.
 */
@Embeddable
public class EventBeneficiaryId implements Serializable {

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "beneficiary_id", nullable = false)
    private UUID beneficiaryId;

    public EventBeneficiaryId() {
    }

    public EventBeneficiaryId(UUID eventId, UUID beneficiaryId) {
        this.eventId = eventId;
        this.beneficiaryId = beneficiaryId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public UUID getBeneficiaryId() {
        return beneficiaryId;
    }

    public void setBeneficiaryId(UUID beneficiaryId) {
        this.beneficiaryId = beneficiaryId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventBeneficiaryId that = (EventBeneficiaryId) o;
        return Objects.equals(eventId, that.eventId)
                && Objects.equals(beneficiaryId, that.beneficiaryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, beneficiaryId);
    }
}
