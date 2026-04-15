package com.example.eventticketingsystem.service;

import com.example.eventticketingsystem.dto.common.PagedResponse;
import com.example.eventticketingsystem.dto.role.AssignPermissionsRequest;
import com.example.eventticketingsystem.dto.role.RoleRequest;
import com.example.eventticketingsystem.dto.role.RoleResponse;

public interface RoleService {

    PagedResponse<RoleResponse> listRoles(int limit, int offset);

    RoleResponse createRole(RoleRequest request);

    RoleResponse getRoleById(Long roleId);

    RoleResponse updateRole(Long roleId, RoleRequest request);

    void deleteRole(Long roleId);

    RoleResponse assignPermissions(Long roleId, AssignPermissionsRequest request);
}

