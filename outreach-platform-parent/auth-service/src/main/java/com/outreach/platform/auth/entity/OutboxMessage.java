package com.outreach.platform.auth.entity;

import com.outreach.platform.common.entity.BaseEntity;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** An identity event waiting to be published to RabbitMQ (transactional outbox). */
@Entity
@Table(name = "auth_outbox")
@Getter
@Setter
public class OutboxMessage extends BaseEntity {

    public static final String PENDING = "PENDING";
    public static final String PUBLISHED = "PUBLISHED";

    @Column(name = "routing_key", nullable = false, length = 100)
    private String routingKey;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Type(JsonType.class)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "status", nullable = false, length = 20)
    private String status = PENDING;

    @Column(name = "published_at")
    private Instant publishedAt;
}
