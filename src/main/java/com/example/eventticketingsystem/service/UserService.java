package com.example.eventticketingsystem.service;

import com.example.eventticketingsystem.dto.common.PagedResponse;
import com.example.eventticketingsystem.dto.user.UserResponse;
import java.util.Set;

public interface UserService {
    PagedResponse<UserResponse> listUsers(int limit, int offset);
    UserResponse getUserById(Long userId);
    UserResponse updateUserRoles(Long userId, Set<String> roles);
    UserResponse updateUser(Long userId, String name, String email);
    void deleteUser(Long userId);
}
