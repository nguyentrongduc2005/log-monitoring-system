package com.vdt.log_monitoring.modules.identity.internal.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApplicationRepository extends JpaRepository<ApplicationEntity, UUID> {

	boolean existsByNameIgnoreCase(String name);

	Optional<ApplicationEntity> findByNameIgnoreCase(String name);

	List<ApplicationEntity> findByStatus(ApplicationStatus status);

	@Query("""
		select application
		from ApplicationEntity application
		where application.status = :status
		  and application.id in (
			select access.id.applicationId
			from UserApplicationAccessEntity access
			where access.id.userId = :userId
		  )
		""")
	List<ApplicationEntity> findVisibleByUserIdAndStatus(
		@Param("userId") UUID userId,
		@Param("status") ApplicationStatus status
	);
}
