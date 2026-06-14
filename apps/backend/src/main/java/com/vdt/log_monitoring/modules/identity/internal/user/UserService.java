package com.vdt.log_monitoring.modules.identity.internal.user;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vdt.log_monitoring.modules.identity.api.IdentityException;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public UserEntity createUser(String email, String rawPassword, String displayName, UserRole role) {
		String normalizedEmail = email.trim().toLowerCase();
		if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
			throw new IdentityException(IdentityException.ErrorCode.EMAIL_ALREADY_EXISTS, "Email is already registered");
		}

		String encodedPassword = passwordEncoder.encode(rawPassword);
		return userRepository.save(UserEntity.create(normalizedEmail, encodedPassword, displayName, role));
	}

	public UserEntity getUserById(UUID id) {
		return userRepository.findById(id)
			.orElseThrow(() -> new IdentityException(IdentityException.ErrorCode.USER_NOT_FOUND, "User not found"));
	}

	public Page<UserEntity> listUsers(
		String search,
		UserRole role,
		UserStatus status,
		boolean includeDeleted,
		Pageable pageable
	) {
		return userRepository.findAll(userSpecification(search, role, status, includeDeleted), pageable);
	}

	public UserEntity getUserByEmail(String email) {
		return userRepository.findByEmailIgnoreCase(email)
			.orElseThrow(() -> new IdentityException(IdentityException.ErrorCode.USER_NOT_FOUND, "User not found"));
	}

	@Transactional
	public UserEntity updateProfile(UUID id, String email, String displayName) {
		UserEntity entity = userRepository.findById(id)
			.orElseThrow(() -> new IdentityException(IdentityException.ErrorCode.USER_NOT_FOUND, "User not found"));

		String normalizedEmail = email.trim().toLowerCase();
		if (!entity.getEmail().equalsIgnoreCase(normalizedEmail) && userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
			throw new IdentityException(IdentityException.ErrorCode.EMAIL_ALREADY_EXISTS, "Email is already registered");
		}

		entity.updateProfile(normalizedEmail, displayName);
		return entity;
	}

	@Transactional
	public UserEntity changeRole(UUID id, UserRole role) {
		UserEntity entity = userRepository.findById(id)
			.orElseThrow(() -> new IdentityException(IdentityException.ErrorCode.USER_NOT_FOUND, "User not found"));

		entity.changeRole(role);
		return entity;
	}

	@Transactional
	public UserEntity changeStatus(UUID id, UserStatus status) {
		UserEntity entity = userRepository.findById(id)
			.orElseThrow(() -> new IdentityException(IdentityException.ErrorCode.USER_NOT_FOUND, "User not found"));

		entity.changeStatus(status);
		return entity;
	}

	@Transactional
	public UserEntity softDelete(UUID id) {
		UserEntity entity = userRepository.findById(id)
			.orElseThrow(() -> new IdentityException(IdentityException.ErrorCode.USER_NOT_FOUND, "User not found"));

		entity.softDelete();
		return entity;
	}

	private Specification<UserEntity> userSpecification(
		String search,
		UserRole role,
		UserStatus status,
		boolean includeDeleted
	) {
		return (root, query, criteriaBuilder) -> {
			var predicate = criteriaBuilder.conjunction();

			if (!includeDeleted) {
				predicate = criteriaBuilder.and(
					predicate,
					criteriaBuilder.notEqual(root.get("status"), UserStatus.DELETED)
				);
			}

			if (role != null) {
				predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("role"), role));
			}

			if (status != null) {
				predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("status"), status));
			}

			if (search != null && !search.isBlank()) {
				String pattern = "%" + search.trim().toLowerCase() + "%";
				predicate = criteriaBuilder.and(
					predicate,
					criteriaBuilder.or(
						criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), pattern),
						criteriaBuilder.like(criteriaBuilder.lower(root.get("displayName")), pattern)
					)
				);
			}

			return predicate;
		};
	}
}
