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

import java.util.UUID;

/**
 * Junction entity for the many-to-many relationship between events and beneficiaries.
 *
 * <p>Uses a composite {@code @EmbeddedId} ({@code event_id} + {@code beneficiary_id}), so unlike
 * every other tenant-scoped entity in this service it cannot extend {@code TenantAwareBaseEntity}
 * (which requires a single {@code UUID id}). It still applies the shared {@code tenantFilter} via
 * {@code @Filter} — but must NOT redeclare {@code @FilterDef} for it: that filter is already
 * registered once via {@code TenantAwareBaseEntity}, and Hibernate treats two independent
 * {@code @FilterDef}s with the same name in one persistence unit as a boot-time conflict
 * ("Multiple '@FilterDef' annotations define a filter named 'tenantFilter'") — this is exactly
 * the bug that surfaced while adding a tenant-isolation regression test, see
 * docs/specs/platform-hardening/. Composite-key entities are a distinct case from the Bucket
 * A/B/C classification in that spec — not a candidate for extending the base class at all.
 */
@Entity
@Table(name = "event_beneficiary")
@EntityListeners(TenantEntityListener.class)
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
