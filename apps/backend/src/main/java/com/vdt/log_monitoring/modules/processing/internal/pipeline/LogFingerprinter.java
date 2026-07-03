package com.vdt.log_monitoring.modules.processing.internal.pipeline;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.processing.api.ProcessingException;
import com.vdt.log_monitoring.modules.processing.internal.model.LogFingerprint;
import com.vdt.log_monitoring.modules.processing.internal.model.ProcessedLog;

@Component
public class LogFingerprinter {

    public ProcessedLog addFingerprint(ProcessedLog log) {
        String source = String.join("|",
                log.applicationId().toString(),
                log.level().name(),
                normalizeMessage(log.message()));
        return log.withFingerprint(LogFingerprint.of(sha256(source)));
    }

    private String normalizeMessage(String message) {
        return message
                .replaceAll("\\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\b",
                        "{uuid}")
                .replaceAll("\\b\\d+\\b", "{number}")
                .strip()
                .toLowerCase();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new ProcessingException(
                    ProcessingException.ErrorCode.PROCESSING_UNAVAILABLE,
                    "SHA-256 fingerprint algorithm is unavailable",
                    ex);
        }
    }
}
