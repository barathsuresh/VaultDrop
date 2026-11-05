package com.vaultdrop.ephemeral_api.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Model of the Data to be stored at runtime
 * SecretEntity
 * @param id
 * @param content
 * @param hashedPassphrase
 * @param expiresAt
 * @param viewsRemaining
 * @author barathsuresh
 */
public record SecretEntity(UUID id, String content, String hashedPassphrase, Instant expiresAt, int viewsRemaining) {
    public boolean isExpired(){
        return Instant.now().isAfter(expiresAt);
    }
}
