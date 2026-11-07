package com.vaultdrop.ephemeral_api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
/**
 * DTOs for Request and Response for Secret Message
 * SecretDtos
 * @author barathsuresh
 */
public class SecretDtos {
    public record CreateSecretRequest(
            @NotBlank(message = "Secret message cannot be blank") @Size(max = 10000, message = "Payload exeeds maximum allowed size of 10KB") String content,

            @Size(max = 128, message = "Passphrase max length is 128 characters") String passPhrase,

            @Min(value = 60, message = "TTL must be atleast 60 seconds") @Max(value = 604800, message = "TTL cannot exceed 7 days") Long ttlSeconds) {
    }

    public record CreateSecretResponse(String id, String accessUrl, long expiresInSeconds) {
    }

    public record RevealSecretResponse(String content, long createdAtEpochMs) {
    }
}
