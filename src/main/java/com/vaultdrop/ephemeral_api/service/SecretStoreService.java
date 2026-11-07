package com.vaultdrop.ephemeral_api.service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.vaultdrop.ephemeral_api.exception.InvalidPassphraseException;
import com.vaultdrop.ephemeral_api.model.SecretEntity;
import com.vaultdrop.ephemeral_api.util.HashingUtil;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SecretStoreService {
    private static final long DEFAULT_TTL_SECONDS = 3600;

    // In-memory thread-safe store
    private final Map<UUID, SecretEntity> store = new ConcurrentHashMap<>();

    /**
     * Stores a new secret in memory
     * 
     * @param content
     * @param rawPassphrase
     * @param ttlSeconds
     * @return
     */
    public UUID save(String content, String rawPassphrase, Long ttlSeconds) {
        UUID id = UUID.randomUUID();

        long resolvedTtl = (ttlSeconds != null && ttlSeconds > 0) ? ttlSeconds : DEFAULT_TTL_SECONDS;

        Instant expiresAt = Instant.now().plusSeconds(resolvedTtl);

        String hashedPassphrase = (rawPassphrase != null && !rawPassphrase.isBlank()) ? HashingUtil.hash(rawPassphrase)
                : null;

        SecretEntity entity = new SecretEntity(id, content, hashedPassphrase, expiresAt, 1);
        store.put(id, entity);

        log.info("Stored secret id={} with TTL={}s", id, resolvedTtl);

        return id;
    }

    public Optional<String> consume(UUID id, String candiatePassphrase) {
        SecretEntity entity = store.get(id);

        if (entity == null) {
            return Optional.empty();
        }

        // check if expired
        if (entity.isExpired()) {
            store.remove(id);
            log.info("Attempted read on expired secret id={}. Removed.", id);
            return Optional.empty();
        }

        // validate passphrase
        if (entity.hashedPassphrase() != null) {
            if (candiatePassphrase == null || !HashingUtil.hash(candiatePassphrase).equals(entity.hashedPassphrase())) {
                log.warn("Unauthorized attempt to access secret id={}",id);
                throw new InvalidPassphraseException("Invalid credentials for requested secret");
            }
        }

        SecretEntity removed = store.remove(id);

        if(removed != null){
            log.info("Secret id={} consumed and purged from memory", id);
            return Optional.of(removed.content());
        }

        return Optional.empty();
    }

    @Scheduled(fixedRate = 60000)
    public void sweepExpiredRecords(){
        int initialSize = store.size();
        store.entrySet().removeIf(entry -> entry.getValue().isExpired());
        int removedCount = initialSize - store.size();

        if(removedCount>0){
            log.info("Eviction sweep completed: purged {} expired secrets. Current pool size: {}", removedCount, store.size());
        }
    }
}
