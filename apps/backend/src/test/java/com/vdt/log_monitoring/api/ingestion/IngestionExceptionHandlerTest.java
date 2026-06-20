package com.vdt.log_monitoring.api.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.vdt.log_monitoring.modules.ingestion.api.IngestionException;

class IngestionExceptionHandlerTest {

    private final IngestionExceptionHandler handler = new IngestionExceptionHandler();

    @Test
    void usesStatusDeclaredByErrorCode() {
        IngestionException exception = new IngestionException(
                IngestionException.ErrorCode.INGESTION_UNAVAILABLE,
                "Failed to publish raw log event");

        var response = handler.handleIngestionException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().getData()).isEqualTo("INGESTION_UNAVAILABLE");
    }
}
