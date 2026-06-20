package com.vdt.log_monitoring.modules.processing;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class ProcessingModuleBoundaryTest {

	private static final Path MAIN_SOURCE = Path.of("src/main/java");
	private static final Path PROCESSING_MODULE = MAIN_SOURCE.resolve(
		"com/vdt/log_monitoring/modules/processing"
	);
	private static final String INTERNAL_IMPORT =
		"com.vdt.log_monitoring.modules.processing.internal";

	@Test
	void codeOutsideProcessingModuleUsesOnlyPublicProcessingApi() throws IOException {
		List<Path> violations;
		try (var files = Files.walk(MAIN_SOURCE)) {
			violations = files
				.filter(path -> path.toString().endsWith(".java"))
				.filter(path -> !path.startsWith(PROCESSING_MODULE))
				.filter(this::importsProcessingInternalPackage)
				.toList();
		}

		assertThat(violations)
			.as("Code outside processing must not import processing.internal")
			.isEmpty();
	}

	private boolean importsProcessingInternalPackage(Path path) {
		try {
			return Files.readString(path).contains(INTERNAL_IMPORT);
		} catch (IOException exception) {
			throw new IllegalStateException("Cannot read " + path, exception);
		}
	}
}
