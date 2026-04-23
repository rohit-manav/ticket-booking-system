package com.example.eventticketingsystem.service;

import com.example.eventticketingsystem.dto.common.PagedResponse;
import com.example.eventticketingsystem.dto.permission.PermissionRequest;
import com.example.eventticketingsystem.dto.permission.PermissionResponse;
import com.example.eventticketingsystem.dto.role.RoleRequest;
import com.example.eventticketingsystem.dto.role.RoleResponse;
import com.example.eventticketingsystem.entity.Permission;
import com.example.eventticketingsystem.entity.Role;
import com.example.eventticketingsystem.repository.PermissionRepository;
import com.example.eventticketingsystem.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RolePermissionCachingTest.TestConfig.class)
class RolePermissionCachingTest {

    @Configuration
    @EnableCaching
    static class TestConfig {
        @Bean
        RoleRepository roleRepository() {
            return Mockito.mock(RoleRepository.class);
        }

        @Bean
        PermissionRepository permissionRepository() {
            return Mockito.mock(PermissionRepository.class);
        }

        @Bean
        RoleServiceImplementation roleService(RoleRepository roleRepository, PermissionRepository permissionRepository) {
            return new RoleServiceImplementation(roleRepository, permissionRepository);
        }

        @Bean
        PermissionServiceImplementation permissionService(PermissionRepository permissionRepository) {
            return new PermissionServiceImplementation(permissionRepository);
        }

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("rolesById", "rolesList", "permissionsById", "permissionsList");
        }
    }

    @org.springframework.beans.factory.annotation.Autowired
    private RoleService roleService;

    @org.springframework.beans.factory.annotation.Autowired
    private PermissionService permissionService;

    @org.springframework.beans.factory.annotation.Autowired
    private RoleRepository roleRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private PermissionRepository permissionRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        // Start each test from a clean cache and clean mock interactions.
        Mockito.reset(roleRepository, permissionRepository);
        clearCache("rolesById");
        clearCache("rolesList");
        clearCache("permissionsById");
        clearCache("permissionsList");
    }

    // -------------------------------------------------------------------------
    // Role caching tests
    // -------------------------------------------------------------------------

    @Test
    void shouldUseRolesByIdCacheWhenFetchingSameRoleTwice() {
        // Arrange
        final long roleId = 1L;
        Role role = role(roleId, "ADMIN");
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        // Act
        RoleResponse first = roleService.getRoleById(roleId);
        RoleResponse second = roleService.getRoleById(roleId);

        // Assert
        assertEquals(first.getId(), second.getId());
        verify(roleRepository, times(1)).findById(roleId);
    }

    @Test
    void shouldUseRolesListCacheForSamePaginationInputs() {
        // Arrange
        Role role = role(1L, "ADMIN");
        when(roleRepository.findAll(eq(PageRequest.of(0, 25))))
                .thenReturn(new PageImpl<>(List.of(role)));

        // Act
        PagedResponse<RoleResponse> first = roleService.listRoles(25, 0);
        PagedResponse<RoleResponse> second = roleService.listRoles(25, 0);

        // Assert
        assertEquals(first.getTotalCount(), second.getTotalCount());
        verify(roleRepository, times(1)).findAll(eq(PageRequest.of(0, 25)));
    }

    @Test
    void shouldEvictRoleByIdCacheWhenRoleIsUpdated() {
        // Arrange
        final long roleId = 10L;
        Role existing = role(roleId, "ADMIN");
        Role updated = role(roleId, "ADMIN_UPDATED");
        RoleRequest request = new RoleRequest();
        request.setName("ADMIN_UPDATED");

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(existing));
        when(roleRepository.findByName("ADMIN_UPDATED")).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenReturn(updated);

        // Warm up cache.
        roleService.getRoleById(roleId);
        roleService.getRoleById(roleId);
        verify(roleRepository, times(1)).findById(roleId);

        // Act: update should evict cache entry for roleId=10.
        roleService.updateRole(roleId, request);
        roleService.getRoleById(roleId);

        // Assert: one additional read happens after eviction.
        verify(roleRepository, times(3)).findById(roleId);
    }

    // -------------------------------------------------------------------------
    // Permission caching tests
    // -------------------------------------------------------------------------

    @Test
    void shouldUsePermissionsByIdCacheWhenFetchingSamePermissionTwice() {
        // Arrange
        final long permissionId = 7L;
        Permission permission = permission(permissionId, "event.read");
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(permission));

        // Act
        PermissionResponse first = permissionService.getPermissionById(permissionId);
        PermissionResponse second = permissionService.getPermissionById(permissionId);

        // Assert
        assertEquals(first.getId(), second.getId());
        verify(permissionRepository, times(1)).findById(permissionId);
    }

    @Test
    void shouldUsePermissionsListCacheForSamePaginationInputs() {
        // Arrange
        Permission permission = permission(7L, "event.read");
        when(permissionRepository.findAll(eq(PageRequest.of(0, 25))))
                .thenReturn(new PageImpl<>(List.of(permission)));

        // Act
        PagedResponse<PermissionResponse> first = permissionService.listPermissions(25, 0);
        PagedResponse<PermissionResponse> second = permissionService.listPermissions(25, 0);

        // Assert
        assertEquals(first.getTotalCount(), second.getTotalCount());
        verify(permissionRepository, times(1)).findAll(eq(PageRequest.of(0, 25)));
    }

    @Test
    void shouldEvictPermissionByIdCacheWhenPermissionIsUpdated() {
        // Arrange
        final long permissionId = 5L;
        Permission existing = permission(permissionId, "event.read");
        Permission updated = permission(permissionId, "event.read.all");
        PermissionRequest request = new PermissionRequest();
        request.setName("event.read.all");

        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(existing));
        when(permissionRepository.findByName("event.read.all")).thenReturn(Optional.empty());
        when(permissionRepository.save(any(Permission.class))).thenReturn(updated);

        // Warm up cache.
        permissionService.getPermissionById(permissionId);
        permissionService.getPermissionById(permissionId);
        verify(permissionRepository, times(1)).findById(permissionId);

        // Act: update should evict cache entry for permissionId=5.
        permissionService.updatePermission(permissionId, request);
        permissionService.getPermissionById(permissionId);

        // Assert: one additional read happens after eviction.
        verify(permissionRepository, times(3)).findById(permissionId);
    }

    private void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    private Role role(Long id, String name) {
        Role role = new Role();
        ReflectionTestUtils.setField(role, "id", id);
        role.setName(name);
        return role;
    }

    private Permission permission(Long id, String name) {
        Permission permission = new Permission();
        ReflectionTestUtils.setField(permission, "id", id);
        permission.setName(name);
        return permission;
    }
}
