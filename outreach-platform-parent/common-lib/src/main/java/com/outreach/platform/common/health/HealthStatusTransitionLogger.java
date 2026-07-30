package com.outreach.platform.common.health;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.health.SystemHealth;
import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Polls health indicators on a schedule and logs warnings when components transition between healthy and unhealthy states.
 */
@Component
public class HealthStatusTransitionLogger {

    private static final Logger log = LoggerFactory.getLogger(HealthStatusTransitionLogger.class);

    private final HealthEndpoint healthEndpoint;
    private final Map<String, Status> previousStatuses = new ConcurrentHashMap<>();

    @Inject
    public HealthStatusTransitionLogger(HealthEndpoint healthEndpoint) {
        this.healthEndpoint = healthEndpoint;
    }

    @Scheduled(fixedDelayString = "${platform.health.poll-interval-ms:30000}")
    public void checkHealthTransitions() {
        try {
            HealthComponent health = healthEndpoint.health();
            if (health instanceof SystemHealth systemHealth) {
                processComponents(systemHealth.getComponents());
            } else if (health instanceof CompositeHealth compositeHealth) {
                processComponents(compositeHealth.getComponents());
            }
        } catch (Exception ex) {
            log.debug("Health transition check encountered an error: {}", ex.getMessage());
        }
    }

    private void processComponents(Map<String, HealthComponent> components) {
        if (components == null) {
            return;
        }

        for (Map.Entry<String, HealthComponent> entry : components.entrySet()) {
            String name = entry.getKey();
            HealthComponent component = entry.getValue();
            Status currentStatus = component.getStatus();

            Status previousStatus = previousStatuses.put(name, currentStatus);

            if (previousStatus == null) {
                // First observation — no transition to report
                continue;
            }

            if (isHealthy(previousStatus) && !isHealthy(currentStatus)) {
                log.warn("Health status transition: {} changed from {} to {}",
                        name, previousStatus.getCode(), currentStatus.getCode());
            } else if (!isHealthy(previousStatus) && isHealthy(currentStatus)) {
                log.info("Health status recovered: {} changed from {} to {}",
                        name, previousStatus.getCode(), currentStatus.getCode());
            }
        }
    }

    private boolean isHealthy(Status status) {
        return Status.UP.equals(status);
    }
}
