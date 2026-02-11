package com.example.zylo.user.service;

import com.example.zylo.user.dto.request.CreateUserRequest;
import com.example.zylo.user.dto.request.UpdateUserRequest;
import com.example.zylo.user.dto.response.UserResponse;

public interface UserService {
    UserResponse createUser(CreateUserRequest request);
    UserResponse getUserById(Long id);
    UserResponse updateUser(Long id, UpdateUserRequest request);
    void deleteUser(Long id);
}
