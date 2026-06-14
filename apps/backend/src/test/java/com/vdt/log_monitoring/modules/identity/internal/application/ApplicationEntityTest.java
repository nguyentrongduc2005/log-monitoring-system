package com.vdt.log_monitoring.modules.identity.internal.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class ApplicationEntityTest {

	@Test
	void createTrimsFieldsAndActivatesApplication() {
		UUID createdBy = UUID.fromString("00000000-0000-0000-0000-000000000001");

		ApplicationEntity application = ApplicationEntity.create(
			" checkout-api ",
			" Checkout API ",
			" Checkout service logs ",
			createdBy
		);

		assertThat(application.getId()).isNotNull();
		assertThat(application.getName()).isEqualTo("checkout-api");
		assertThat(application.getDisplayName()).isEqualTo("Checkout API");
		assertThat(application.getDescription()).isEqualTo("Checkout service logs");
		assertThat(application.getStatus()).isEqualTo(ApplicationStatus.ACTIVE);
		assertThat(application.getCreatedBy()).isEqualTo(createdBy);
		assertThat(application.getCreatedAt()).isNotNull();
		assertThat(application.getUpdatedAt()).isNotNull();
	}

	@Test
	void updatesApplicationState() {
		ApplicationEntity application = ApplicationEntity.create(
			"checkout-api",
			"Checkout API",
			null,
			UUID.fromString("00000000-0000-0000-0000-000000000001")
		);

		application.update(" checkout-v2 ", " Checkout V2 ", " Updated ");
		application.changeStatus(ApplicationStatus.INACTIVE);

		assertThat(application.getName()).isEqualTo("checkout-v2");
		assertThat(application.getDisplayName()).isEqualTo("Checkout V2");
		assertThat(application.getDescription()).isEqualTo("Updated");
		assertThat(application.getStatus()).isEqualTo(ApplicationStatus.INACTIVE);
	}
}
