package com.banking.service;

import com.banking.dto.response.TransferReceiptResponse;
import com.banking.entity.IdempotencyKey;
import com.banking.entity.User;
import com.banking.entity.enums.IdempotencyStatus;
import com.banking.exception.DuplicateIdempotencyKeyException;
import com.banking.repository.IdempotencyKeyRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyKeyRepository idempotencyKeyRepository) {
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    /**
     * Compute SHA-256 hash of the payload to detect modifications if the same idempotency key is reused.
     */
    public String computePayloadHash(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((payload != null ? payload : "").getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Inspects the idempotency key in an isolated transaction.
     * Returns cached response if already completed, or reserves the key as IN_PROGRESS.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TransferReceiptResponse getOrLockKey(User user, String key, String payloadHash) {
        if (key == null || key.trim().isEmpty()) {
            return null; // Idempotency key is optional; if omitted, no caching
        }

        Optional<IdempotencyKey> existing = idempotencyKeyRepository.findByUserIdAndIdempotencyKey(user.getId(), key);

        if (existing.isPresent()) {
            IdempotencyKey record = existing.get();

            // Guard against reusing the same key for a different request
            if (!record.getRequestHash().equals(payloadHash)) {
                log.warn("Idempotency key '{}' reused with different payload for user '{}'", key, user.getUsername());
                throw new DuplicateIdempotencyKeyException(
                        "Idempotency key has already been used for a different transfer request");
            }

            if (record.getStatus() == IdempotencyStatus.COMPLETED) {
                log.info("Idempotent replay detected for key '{}'. Returning cached transfer receipt.", key);
                try {
                    return objectMapper.readValue(record.getResponseBody(), TransferReceiptResponse.class);
                } catch (JsonProcessingException e) {
                    log.error("Failed to deserialize cached idempotency response", e);
                    throw new IllegalStateException("Cached response deserialization failure", e);
                }
            } else if (record.getStatus() == IdempotencyStatus.IN_PROGRESS) {
                log.warn("Concurrent duplicate request detected for in-flight key '{}'", key);
                throw new DuplicateIdempotencyKeyException(
                        "A transfer with this idempotency key is currently processing. Please do not retry immediately.");
            } else {
                // Previously failed attempt; allow retry by marking IN_PROGRESS
                record.setStatus(IdempotencyStatus.IN_PROGRESS);
                idempotencyKeyRepository.save(record);
                return null;
            }
        }

        // New key: reserve it in IN_PROGRESS state
        try {
            IdempotencyKey newRecord = new IdempotencyKey(key, user, payloadHash, IdempotencyStatus.IN_PROGRESS);
            idempotencyKeyRepository.saveAndFlush(newRecord);
            return null;
        } catch (DataIntegrityViolationException ex) {
            // Concurrent insert race condition caught by DB unique constraint uk_user_idempotency_key
            throw new DuplicateIdempotencyKeyException("Duplicate transfer request detected in flight");
        }
    }

    /**
     * Stores the completed response in the idempotency record.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(User user, String key, TransferReceiptResponse response) {
        if (key == null || key.trim().isEmpty()) {
            return;
        }
        idempotencyKeyRepository.findByUserIdAndIdempotencyKey(user.getId(), key).ifPresent(record -> {
            try {
                record.setStatus(IdempotencyStatus.COMPLETED);
                record.setResponseCode(200);
                record.setResponseBody(objectMapper.writeValueAsString(response));
                idempotencyKeyRepository.save(record);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize transfer receipt for idempotency key '{}'", key, e);
            }
        });
    }

    /**
     * Marks the idempotency key as FAILED if an unrecoverable business failure occurred.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(User user, String key) {
        if (key == null || key.trim().isEmpty()) {
            return;
        }
        idempotencyKeyRepository.findByUserIdAndIdempotencyKey(user.getId(), key).ifPresent(record -> {
            record.setStatus(IdempotencyStatus.FAILED);
            idempotencyKeyRepository.save(record);
        });
    }
}
