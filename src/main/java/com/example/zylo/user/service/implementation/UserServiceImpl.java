package com.example.zylo.user.service.implementation;

import com.example.zylo.user.dto.request.CreateUserRequest;
import com.example.zylo.user.dto.request.UpdateUserRequest;
import com.example.zylo.user.dto.response.UserResponse;
import com.example.zylo.user.entity.UserEntity;
import com.example.zylo.user.exception.DuplicateFieldException;
import com.example.zylo.user.exception.UserNotFoundException;
import com.example.zylo.user.repository.UserRepository;
import com.example.zylo.user.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest req) {
        UserEntity user = UserEntity.builder()
                .email(req.getEmail())
                .name(req.getName())
                .password(req.getPassword())    // TODO: use Bcrypt package to hash password!
                .build();

        UserEntity savedUser = userRepository.save(user);

        return mapToResponse(savedUser);
    };

    private UserResponse mapToResponse(UserEntity user) {
        // Entity to DTO conversion
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }

    @Override
    public UserResponse getUserById(Long id) {
        UserEntity user = userRepository.findByUserId(id).orElseThrow(() -> new UserNotFoundException("user with " + id + " not found" ));

        return mapToResponse(user);
    }


    @Override
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        UserEntity user = userRepository.findByUserId(id).orElseThrow(() -> new UserNotFoundException("user with " + id + " not found" ));

        // update name
        if (user.getName() != null && !request.getName().isBlank()) {
            if (userRepository.existsByUsernameAndIdNot(request.getName(), request.getId())) {
                throw new DuplicateFieldException("Username already exists");
            }

            user.setName(request.getName());
        }

        // update email
        if(user.getEmail() != null && !request.getEmail().isBlank()) {
            if(userRepository.existsByEmailAndIdNot(request.getEmail(), request.getId())) {
                throw new DuplicateFieldException("Email already exists");
            }
        }

        UserEntity updatedUser = userRepository.save(user);

        return mapToResponse(updatedUser);
    };


    @Transactional
    @Override
    public void deleteUser(Long id) {
        UserEntity user = userRepository.findByUserId(id).orElseThrow(() -> new UserNotFoundException("user with " + id + " not found" ));

        // soft delete
        user.setIsDeleted(true);
        user.setIsActive(false);
    }

}
