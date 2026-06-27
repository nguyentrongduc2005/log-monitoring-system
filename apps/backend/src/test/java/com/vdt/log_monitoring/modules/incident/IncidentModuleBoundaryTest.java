package com.vdt.log_monitoring.modules.incident;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class IncidentModuleBoundaryTest {

	private static final Path MAIN_SOURCE = Path.of("src/main/java");
	private static final Path INCIDENT_MODULE = MAIN_SOURCE.resolve(
		"com/vdt/log_monitoring/modules/incident"
	);
	private static final String INTERNAL_IMPORT =
		"com.vdt.log_monitoring.modules.incident.internal";

	@Test
	void codeOutsideIncidentModuleUsesOnlyPublicIncidentApi() throws IOException {
		List<Path> violations;
		try (var files = Files.walk(MAIN_SOURCE)) {
			violations = files
				.filter(path -> path.toString().endsWith(".java"))
				.filter(path -> !path.startsWith(INCIDENT_MODULE))
				.filter(this::importsIncidentInternalPackage)
				.toList();
		}

		assertThat(violations)
			.as("Code outside incident must not import incident.internal")
			.isEmpty();
	}

	private boolean importsIncidentInternalPackage(Path path) {
		try {
			return Files.readString(path).contains(INTERNAL_IMPORT);
		} catch (IOException exception) {
			throw new IllegalStateException("Cannot read " + path, exception);
		}
	}
}
