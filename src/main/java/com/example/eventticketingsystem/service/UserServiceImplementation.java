package com.example.eventticketingsystem.service;

import com.example.eventticketingsystem.dto.common.PagedResponse;
import com.example.eventticketingsystem.dto.user.UserResponse;
import com.example.eventticketingsystem.entity.Role;
import com.example.eventticketingsystem.entity.User;
import com.example.eventticketingsystem.repository.RoleRepository;
import com.example.eventticketingsystem.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserServiceImplementation implements UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public UserServiceImplementation(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> listUsers(int limit, int offset) {
        List<User> users = userRepository.findAll().stream()
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
        long total = userRepository.count();
        List<UserResponse> items = users.stream().map(this::toUserResponse).collect(Collectors.toList());
        return new PagedResponse<>(items, limit, offset, total);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return toUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateUserRoles(Long userId, Set<String> roles) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Set<Role> newRoles = roles.stream()
                .map(roleName -> roleRepository.findByName(roleName)
                        .orElseThrow(() -> new RuntimeException("Role not found: " + roleName)))
                .collect(Collectors.toSet());
        user.setRoles(newRoles);
        userRepository.save(user);
        return toUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long userId, String name, String email) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setName(name);
        user.setEmail(email);
        userRepository.save(user);
        return toUserResponse(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        userRepository.delete(user);
    }

    private UserResponse toUserResponse(User user) {
        Set<String> roleNames = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), roleNames);
    }
}
