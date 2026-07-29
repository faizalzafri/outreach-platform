package com.outreach.platform.event.entity;

import com.outreach.platform.common.tenant.TenantConstants;
import com.outreach.platform.common.tenant.TenantEntityListener;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.util.UUID;

/**
 * Junction entity for the many-to-many relationship between events and beneficiaries.
 */
@Entity
@Table(name = "event_beneficiary")
@EntityListeners(TenantEntityListener.class)
@FilterDef(name = TenantConstants.TENANT_FILTER_NAME, parameters = @ParamDef(name = "tenantId", type = UUID.class))
@Filter(name = TenantConstants.TENANT_FILTER_NAME, condition = "tenant_id = :tenantId")
public class EventBeneficiaryEntity {

    @EmbeddedId
    private EventBeneficiaryId id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

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

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
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
