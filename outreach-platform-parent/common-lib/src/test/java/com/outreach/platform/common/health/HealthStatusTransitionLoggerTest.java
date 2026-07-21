package com.outreach.platform.common.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.health.SystemHealth;

import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HealthStatusTransitionLogger")
class HealthStatusTransitionLoggerTest {

    @Mock
    private HealthEndpoint healthEndpoint;

    private HealthStatusTransitionLogger logger;

    @BeforeEach
    void setUp() {
        logger = new HealthStatusTransitionLogger(healthEndpoint);
    }

    @Test
    @DisplayName("does not throw on first health check (establishes baseline)")
    void firstCheckEstablishesBaseline() {
        SystemHealth systemHealth = mockSystemHealth(Map.of(
                "db", Health.up().build(),
                "smtp", Health.up().build()
        ));
        when(healthEndpoint.health()).thenReturn(systemHealth);

        // Should not throw
        logger.checkHealthTransitions();
    }

    @Test
    @DisplayName("detects transition from UP to DOWN without errors")
    void detectsTransitionFromUpToDown() {
        // First call: all UP
        SystemHealth healthyState = mockSystemHealth(Map.of(
                "db", Health.up().build(),
                "smtp", Health.up().build()
        ));
        when(healthEndpoint.health()).thenReturn(healthyState);
        logger.checkHealthTransitions();

        // Second call: smtp goes DOWN
        SystemHealth degradedState = mockSystemHealth(Map.of(
                "db", Health.up().build(),
                "smtp", Health.down().build()
        ));
        when(healthEndpoint.health()).thenReturn(degradedState);

        // Should not throw — just logs a warning
        logger.checkHealthTransitions();
    }

    @Test
    @DisplayName("detects recovery from DOWN to UP without errors")
    void detectsRecovery() {
        // First call: smtp DOWN
        SystemHealth degradedState = mockSystemHealth(Map.of(
                "db", Health.up().build(),
                "smtp", Health.down().build()
        ));
        when(healthEndpoint.health()).thenReturn(degradedState);
        logger.checkHealthTransitions();

        // Second call: smtp recovers
        SystemHealth healthyState = mockSystemHealth(Map.of(
                "db", Health.up().build(),
                "smtp", Health.up().build()
        ));
        when(healthEndpoint.health()).thenReturn(healthyState);

        // Should not throw — just logs info
        logger.checkHealthTransitions();
    }

    @Test
    @DisplayName("handles null components gracefully")
    void handlesNullComponents() {
        SystemHealth systemHealth = mock(SystemHealth.class);
        when(systemHealth.getComponents()).thenReturn(null);
        when(healthEndpoint.health()).thenReturn(systemHealth);

        // Should not throw
        logger.checkHealthTransitions();
    }

    @Test
    @DisplayName("handles health endpoint exceptions gracefully")
    void handlesExceptionsGracefully() {
        when(healthEndpoint.health()).thenThrow(new RuntimeException("Health endpoint unavailable"));

        // Should not throw
        logger.checkHealthTransitions();
    }

    private SystemHealth mockSystemHealth(Map<String, HealthComponent> components) {
        SystemHealth systemHealth = mock(SystemHealth.class);
        when(systemHealth.getComponents()).thenReturn(components);
        return systemHealth;
    }
}
