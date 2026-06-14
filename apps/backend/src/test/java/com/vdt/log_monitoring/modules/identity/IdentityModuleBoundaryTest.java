package com.vdt.log_monitoring.modules.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class IdentityModuleBoundaryTest {

	private static final Path MAIN_SOURCE = Path.of("src/main/java");
	private static final Path IDENTITY_MODULE = MAIN_SOURCE.resolve(
		"com/vdt/log_monitoring/modules/identity"
	);
	private static final String INTERNAL_IMPORT =
		"com.vdt.log_monitoring.modules.identity.internal";

	@Test
	void codeOutsideIdentityModuleUsesOnlyPublicIdentityApi() throws IOException {
		List<Path> violations;
		try (var files = Files.walk(MAIN_SOURCE)) {
			violations = files
				.filter(path -> path.toString().endsWith(".java"))
				.filter(path -> !path.startsWith(IDENTITY_MODULE))
				.filter(this::importsIdentityInternalPackage)
				.toList();
		}

		assertThat(violations)
			.as("Code outside identity must not import identity.internal")
			.isEmpty();
	}

	private boolean importsIdentityInternalPackage(Path path) {
		try {
			return Files.readString(path).contains(INTERNAL_IMPORT);
		} catch (IOException exception) {
			throw new IllegalStateException("Cannot read " + path, exception);
		}
	}
}
