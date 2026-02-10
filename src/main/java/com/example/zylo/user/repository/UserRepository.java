package com.example.zylo.user.repository;

import com.example.zylo.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByUserName(String name);

    @Query("Select u From User u WHERE u.isDeleted = false AND u.id = :id")
    Optional<UserEntity> findByUserId(Long id);

    boolean isEmailExist(String email);
    boolean isUserNameExist(String name);
    boolean existsByUsernameAndIdNot(String name, Long id);
    boolean existsByEmailAndIdNot(String email, Long id);
}
