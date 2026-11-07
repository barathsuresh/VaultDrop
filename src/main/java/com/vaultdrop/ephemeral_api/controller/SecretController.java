package com.vaultdrop.ephemeral_api.controller;

import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vaultdrop.ephemeral_api.dto.SecretDtos.CreateSecretRequest;
import com.vaultdrop.ephemeral_api.dto.SecretDtos.CreateSecretResponse;
import com.vaultdrop.ephemeral_api.dto.SecretDtos.RevealSecretResponse;
import com.vaultdrop.ephemeral_api.service.SecretStoreService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/secrets")
public class SecretController {
    private final SecretStoreService secretStoreService;

    public SecretController(SecretStoreService secretStoreService) {
        this.secretStoreService = secretStoreService;
    }

    @PostMapping
    public ResponseEntity<CreateSecretResponse> createSecret(@Valid @RequestBody CreateSecretRequest request) {
        UUID id = secretStoreService.save(request.content(), request.passPhrase(), request.ttlSeconds());

        long effectiveTtl = (request.ttlSeconds() != null) ? request.ttlSeconds() : 3600;
        String accessUrl = "/api/v1/secrets/" + id;

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreateSecretResponse(id.toString(), accessUrl, effectiveTtl));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RevealSecretResponse> getSecret(@PathVariable UUID id,
            @RequestHeader(value = "X-Secret-Passphrase", required = false) String passphrase) {
        return secretStoreService.consume(id, passphrase)
                .map(content -> ResponseEntity.ok(new RevealSecretResponse(content, Instant.now().toEpochMilli())))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
