package com.vdt.log_monitoring.identity;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vdt.log_monitoring.identity.model.UserEntity;
import com.vdt.log_monitoring.identity.model.UserStatus;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

	Optional<UserEntity> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);

	long countByStatus(UserStatus status);
}
