package com.vdt.log_monitoring.modules.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class IngestionModuleBoundaryTest {

	private static final Path MAIN_SOURCE = Path.of("src/main/java");
	private static final Path INGESTION_MODULE = MAIN_SOURCE.resolve(
		"com/vdt/log_monitoring/modules/ingestion"
	);
	private static final String INTERNAL_IMPORT =
		"com.vdt.log_monitoring.modules.ingestion.internal";

	@Test
	void codeOutsideIngestionModuleUsesOnlyPublicIngestionApi() throws IOException {
		List<Path> violations;
		try (var files = Files.walk(MAIN_SOURCE)) {
			violations = files
				.filter(path -> path.toString().endsWith(".java"))
				.filter(path -> !path.startsWith(INGESTION_MODULE))
				.filter(this::importsIngestionInternalPackage)
				.toList();
		}

		assertThat(violations)
			.as("Code outside ingestion must not import ingestion.internal")
			.isEmpty();
	}

	private boolean importsIngestionInternalPackage(Path path) {
		try {
			return Files.readString(path).contains(INTERNAL_IMPORT);
		} catch (IOException exception) {
			throw new IllegalStateException("Cannot read " + path, exception);
		}
	}
}
