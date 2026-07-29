package com.outreach.platform.event.controller;

import com.outreach.platform.event.entity.ResourcePermission;
import com.outreach.platform.event.model.ResourceType;
import com.outreach.platform.event.model.dto.ChangeVisibilityRequest;
import com.outreach.platform.event.model.dto.PermissionResponse;
import com.outreach.platform.event.model.dto.ShareResourceRequest;
import com.outreach.platform.event.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for fine-grained resource permission management.
 * Provides endpoints for sharing resources with teams, changing visibility,
 * listing permissions, and revoking access.
 *
 * <p>Permission checks are enforced at the service layer via {@link PermissionService}.
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/resources/{type}/{id}")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Resource Permissions", description = "Share resources, manage visibility, and control access")
public class ResourcePermissionController {

    private final PermissionService permissionService;

    @Inject
    public ResourcePermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Operation(summary = "Share resource with a team",
            description = "Shares a resource with a team at the specified permission level. Requires MANAGE permission or ownership.")
    @PostMapping("/share")
    public ResponseEntity<Void> shareResource(
            @Parameter(description = "Resource type (SEQUENCE, TEMPLATE, CONTACT_LIST)")
            @PathVariable("type") ResourceType type,
            @Parameter(description = "Resource UUID")
            @PathVariable("id") UUID id,
            @Valid @RequestBody ShareResourceRequest request) {
        permissionService.shareWithTeam(id, type, request.teamId(), request.permissionLevel());
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @Operation(summary = "Change resource visibility",
            description = "Changes the visibility level of a resource. Requires MANAGE permission or ownership.")
    @PutMapping("/visibility")
    public ResponseEntity<Void> changeVisibility(
            @Parameter(description = "Resource type (SEQUENCE, TEMPLATE, CONTACT_LIST)")
            @PathVariable("type") ResourceType type,
            @Parameter(description = "Resource UUID")
            @PathVariable("id") UUID id,
            @Valid @RequestBody ChangeVisibilityRequest request) {
        permissionService.changeVisibility(id, type, request.visibility());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "List resource permissions",
            description = "Returns all permission records for the specified resource. Requires at least VIEW access.")
    @GetMapping("/permissions")
    public ResponseEntity<List<PermissionResponse>> listPermissions(
            @Parameter(description = "Resource type (SEQUENCE, TEMPLATE, CONTACT_LIST)")
            @PathVariable("type") ResourceType type,
            @Parameter(description = "Resource UUID")
            @PathVariable("id") UUID id) {
        List<ResourcePermission> permissions = permissionService.listPermissions(id, type);
        List<PermissionResponse> response = permissions.stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Revoke permission",
            description = "Revokes a specific permission record. Requires MANAGE permission or ownership.")
    @DeleteMapping("/permissions/{permissionId}")
    public ResponseEntity<Void> revokePermission(
            @Parameter(description = "Resource type (SEQUENCE, TEMPLATE, CONTACT_LIST)")
            @PathVariable("type") ResourceType type,
            @Parameter(description = "Resource UUID")
            @PathVariable("id") UUID id,
            @Parameter(description = "Permission record UUID to revoke")
            @PathVariable UUID permissionId) {
        permissionService.revokePermission(permissionId);
        return ResponseEntity.noContent().build();
    }

    private PermissionResponse toResponse(ResourcePermission permission) {
        return new PermissionResponse(
                permission.getId(),
                permission.getResourceType(),
                permission.getResourceId(),
                permission.getVisibility(),
                permission.getOwnerUserId(),
                permission.getGrantedTeamId(),
                permission.getPermissionLevel()
        );
    }
}
