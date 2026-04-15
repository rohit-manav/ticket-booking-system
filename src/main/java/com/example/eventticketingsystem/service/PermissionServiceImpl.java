package com.example.eventticketingsystem.service;

import com.example.eventticketingsystem.dto.common.PagedResponse;
import com.example.eventticketingsystem.dto.permission.PermissionRequest;
import com.example.eventticketingsystem.dto.permission.PermissionResponse;
import com.example.eventticketingsystem.entity.Permission;
import com.example.eventticketingsystem.exception.ConflictException;
import com.example.eventticketingsystem.exception.ResourceNotFoundException;
import com.example.eventticketingsystem.repository.PermissionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;

    public PermissionServiceImpl(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PermissionResponse> listPermissions(int limit, int offset) {
        int page = offset / Math.max(limit, 1);
        Page<Permission> permPage = permissionRepository.findAll(PageRequest.of(page, limit));

        List<PermissionResponse> items = permPage.getContent().stream()
                .map(this::toResponse)
                .toList();

        return new PagedResponse<>(items, limit, offset, permPage.getTotalElements());
    }

    @Override
    @Transactional
    public PermissionResponse createPermission(PermissionRequest request) {
        // Check for duplicate name
        if (permissionRepository.existsByName(request.getName())) {
            throw new ConflictException("DuplicatePermissionName",
                    "A permission with name '" + request.getName() + "' already exists.");
        }

        Permission permission = new Permission();
        permission.setName(request.getName());
        permission = permissionRepository.save(permission);

        return toResponse(permission);
    }

    @Override
    @Transactional(readOnly = true)
    public PermissionResponse getPermissionById(Long permissionId) {
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "The permission with id '" + permissionId + "' was not found."));
        return toResponse(permission);
    }

    @Override
    @Transactional
    public PermissionResponse updatePermission(Long permissionId, PermissionRequest request) {
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "The permission with id '" + permissionId + "' was not found."));

        // Check for duplicate name (excluding current permission)
        permissionRepository.findByName(request.getName()).ifPresent(existing -> {
            if (!existing.getId().equals(permissionId)) {
                throw new ConflictException("DuplicatePermissionName",
                        "A permission with name '" + request.getName() + "' already exists.");
            }
        });

        permission.setName(request.getName());
        permission = permissionRepository.save(permission);

        return toResponse(permission);
    }

    @Override
    @Transactional
    public void deletePermission(Long permissionId) {
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "The permission with id '" + permissionId + "' was not found."));

        // Check if permission is assigned to any roles
        if (!permission.getRoles().isEmpty()) {
            throw new ConflictException("PermissionInUse",
                    "Permission '" + permission.getName() + "' cannot be deleted because it is assigned to one or more roles.");
        }

        permissionRepository.delete(permission);
    }

    private PermissionResponse toResponse(Permission permission) {
        return new PermissionResponse(
                permission.getId(),
                permission.getName(),
                permission.getCreatedAt(),
                permission.getUpdatedAt()
        );
    }
}

