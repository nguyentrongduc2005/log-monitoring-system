package com.vdt.log_monitoring.modules.identity.internal.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRepository extends JpaRepository<UserEntity, UUID>, JpaSpecificationExecutor<UserEntity> {

	Optional<UserEntity> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);
}
