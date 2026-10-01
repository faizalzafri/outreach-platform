package com.outreach.platform.auth.repo;

import com.outreach.platform.auth.entity.OutboxMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, UUID> {

    List<OutboxMessage> findByStatusOrderByCreatedDate(String status, Pageable pageable);

    List<OutboxMessage> findByRoutingKeyOrderByCreatedDate(String routingKey);
}
