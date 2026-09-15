package com.outreach.platform.event.entity;

import com.outreach.platform.common.pii.AesEncryptionConverter;
import com.outreach.platform.common.pii.PiiField;
import com.outreach.platform.common.tenant.TenantAwareBaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Beneficiary organization that receives outreach events.
 *
 * <p>See {@link EventEntity} for why the audit columns are overridden rather than renamed.
 */
@Entity
@Table(name = "beneficiaries")
@AttributeOverride(name = "createdDate", column = @Column(name = "created_at", nullable = false, updatable = false))
@AttributeOverride(name = "lastModifiedDate", column = @Column(name = "updated_at"))
@AttributeOverride(name = "lastModifiedBy", column = @Column(name = "updated_by", length = 100))
public class BeneficiaryEntity extends TenantAwareBaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "organization")
    private String organization;

    @PiiField(description = "Beneficiary contact email")
    @Convert(converter = AesEncryptionConverter.class)
    @Column(name = "contact_email_encrypted")
    private String contactEmail;

    @PiiField(description = "Beneficiary contact phone")
    @Convert(converter = AesEncryptionConverter.class)
    @Column(name = "contact_phone_encrypted")
    private String contactPhone;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "address")
    private String address;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active;

    public BeneficiaryEntity() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    // id, tenantId, createdDate/lastModifiedDate/createdBy/lastModifiedBy, and version
    // are inherited — see the class-level @AttributeOverrides.
}
