package com.example.eventticketingsystem.service;

import com.example.eventticketingsystem.dto.common.PagedResponse;
import com.example.eventticketingsystem.dto.permission.PermissionRequest;
import com.example.eventticketingsystem.dto.permission.PermissionResponse;

public interface PermissionService {

    PagedResponse<PermissionResponse> listPermissions(int limit, int offset);

    PermissionResponse createPermission(PermissionRequest request);

    PermissionResponse getPermissionById(Long permissionId);

    PermissionResponse updatePermission(Long permissionId, PermissionRequest request);

    void deletePermission(Long permissionId);
}

