package com.outreach.platform.notification.repo;

import com.outreach.platform.notification.model.NotificationPreferenceDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * MongoDB repository for notification preference documents.
 */
@Repository
public interface NotificationPreferenceRepository extends MongoRepository<NotificationPreferenceDocument, String> {

    Optional<NotificationPreferenceDocument> findByEmployeeId(String employeeId);
}
