package com.outreach.platform.event.config;

import com.outreach.platform.common.tenant.TenantContext;
import com.outreach.platform.event.service.AuditLogService;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;

/** AOP aspect that logs all Platform_Admin cross-tenant controller access to MongoDB via AuditLogService. */
@Aspect
@Component
public class PlatformAdminAuditAspect {

    private static final Logger log = LoggerFactory.getLogger(PlatformAdminAuditAspect.class);
    private static final String PLATFORM_ADMIN_AUTHORITY = "ROLE_PLATFORM_ADMIN";

    private final AuditLogService auditLogService;

    @Inject
    public PlatformAdminAuditAspect(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    /** Pointcut targeting all public methods in the controller package. */
    @Pointcut("execution(* com.outreach.platform.event.controller..*(..))")
    public void controllerMethods() {
        // pointcut definition
    }


    @AfterReturning("controllerMethods()")
    public void auditPlatformAdminAccess(JoinPoint joinPoint) {
        if (!isPlatformAdmin()) {
            return;
        }

        try {
            String adminUserId = getAdminUserId();
            String action = joinPoint.getSignature().getName();
            String targetTenantId = resolveTargetTenantId();
            String endpoint = resolveEndpoint();

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("className", joinPoint.getSignature().getDeclaringType().getSimpleName());
            metadata.put("httpMethod", resolveHttpMethod());

            auditLogService.logCrossTenantAccess(
                    adminUserId,
                    targetTenantId,
                    action,
                    endpoint,
                    metadata
            );
        } catch (Exception e) {
            // Non-blocking: audit logging failures must not break business operations
            log.warn("Failed to log Platform_Admin audit entry: {}", e.getMessage());
        }
    }

    private boolean isPlatformAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(PLATFORM_ADMIN_AUTHORITY::equals);
    }

    private String getAdminUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "unknown";
    }

    /** Resolves target tenant from query param, TenantContext, or "ALL" for cross-tenant. */
    private String resolveTargetTenantId() {
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            String tenantIdParam = request.getParameter("tenantId");
            if (tenantIdParam != null && !tenantIdParam.isBlank()) {
                return tenantIdParam;
            }
        }

        if (TenantContext.isPresent()) {
            return TenantContext.getCurrentTenantId().toString();
        }

        return "ALL";
    }

    private String resolveEndpoint() {
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            return request.getMethod() + " " + request.getRequestURI();
        }
        return "unknown";
    }

    private String resolveHttpMethod() {
        HttpServletRequest request = getCurrentRequest();
        return request != null ? request.getMethod() : "unknown";
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }
}
