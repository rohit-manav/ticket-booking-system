package com.example.eventticketingsystem.service;

import com.example.eventticketingsystem.dto.common.PagedResponse;
import com.example.eventticketingsystem.dto.role.AssignPermissionsRequest;
import com.example.eventticketingsystem.dto.role.RoleRequest;
import com.example.eventticketingsystem.dto.role.RoleResponse;
import com.example.eventticketingsystem.entity.Permission;
import com.example.eventticketingsystem.entity.Role;
import com.example.eventticketingsystem.exception.ConflictException;
import com.example.eventticketingsystem.exception.ResourceNotFoundException;
import com.example.eventticketingsystem.repository.PermissionRepository;
import com.example.eventticketingsystem.repository.RoleRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RoleServiceImplementation implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RoleServiceImplementation(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "rolesList", key = "#limit + ':' + #offset")
    public PagedResponse<RoleResponse> listRoles(int limit, int offset) {
        int page = offset / Math.max(limit, 1);
        Page<Role> rolePage = roleRepository.findAll(PageRequest.of(page, limit));

        List<RoleResponse> items = rolePage.getContent().stream()
                .map(this::toResponse)
                .toList();

        return new PagedResponse<>(items, limit, offset, rolePage.getTotalElements());
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "rolesList", allEntries = true)
    public RoleResponse createRole(RoleRequest request) {
        if (roleRepository.existsByName(request.getName())) {
            throw new ConflictException("DuplicateRoleName",
                    "A role with name '" + request.getName() + "' already exists.");
        }

        Role role = new Role();
        role.setName(request.getName());
        role = roleRepository.save(role);

        return toResponse(role);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "rolesById", key = "#roleId")
    public RoleResponse getRoleById(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "The role with id '" + roleId + "' was not found."));
        return toResponse(role);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "rolesById", key = "#roleId"),
            @CacheEvict(cacheNames = "rolesList", allEntries = true)
    })
    public RoleResponse updateRole(Long roleId, RoleRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "The role with id '" + roleId + "' was not found."));

        roleRepository.findByName(request.getName()).ifPresent(existing -> {
            if (!existing.getId().equals(roleId)) {
                throw new ConflictException("DuplicateRoleName",
                        "A role with name '" + request.getName() + "' already exists.");
            }
        });

        role.setName(request.getName());
        role = roleRepository.save(role);

        return toResponse(role);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "rolesById", key = "#roleId"),
            @CacheEvict(cacheNames = "rolesList", allEntries = true)
    })
    public void deleteRole(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "The role with id '" + roleId + "' was not found."));

        if (!role.getUsers().isEmpty()) {
            throw new ConflictException("RoleInUse",
                    "Role '" + role.getName() + "' cannot be deleted because it is assigned to one or more users.");
        }

        roleRepository.delete(role);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "rolesById", key = "#roleId"),
            @CacheEvict(cacheNames = "rolesList", allEntries = true)
    })
    public RoleResponse assignPermissions(Long roleId, AssignPermissionsRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "The role with id '" + roleId + "' was not found."));

        Set<Permission> permissions = new HashSet<>();
        for (Long permissionId : request.getPermissionIds()) {
            Permission permission = permissionRepository.findById(permissionId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "The permission with id '" + permissionId + "' was not found."));
            permissions.add(permission);
        }

        role.setPermissions(permissions);
        role = roleRepository.save(role);

        return toResponse(role);
    }

    private RoleResponse toResponse(Role role) {
        List<String> permissionNames = role.getPermissions().stream()
                .map(Permission::getName)
                .sorted()
                .toList();

        return new RoleResponse(role.getId(), role.getName(), permissionNames);
    }
}

