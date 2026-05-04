package com.circleguard.identity.service;

import com.circleguard.identity.model.IdentityMapping;
import com.circleguard.identity.repository.IdentityMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdentityVaultServiceTest {

    @Mock
    private IdentityMappingRepository repository;

    @InjectMocks
    private IdentityVaultService identityVaultService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(identityVaultService, "hashSalt", "test-salt");
    }

    @Test
    void getOrCreateAnonymousId_WhenIdentityExists_ShouldReturnExistingUuid() {
        UUID existingId = UUID.randomUUID();
        IdentityMapping mapping = new IdentityMapping();
        mapping.setAnonymousId(existingId);

        when(repository.findByIdentityHash(anyString())).thenReturn(Optional.of(mapping));

        UUID resultId = identityVaultService.getOrCreateAnonymousId("jane.doe@example.com");

        assertEquals(existingId, resultId);
        verify(repository, never()).save(any());
    }

    @Test
    void getOrCreateAnonymousId_WhenIdentityNew_ShouldCreateAndSave() {
        UUID newId = UUID.randomUUID();
        when(repository.findByIdentityHash(anyString())).thenReturn(Optional.empty());
        
        when(repository.save(any(IdentityMapping.class))).thenAnswer(invocation -> {
            IdentityMapping saved = invocation.getArgument(0);
            saved.setAnonymousId(newId);
            return saved;
        });

        UUID resultId = identityVaultService.getOrCreateAnonymousId("john.doe@example.com");

        assertEquals(newId, resultId);
        verify(repository, times(1)).save(any(IdentityMapping.class));
    }

    @Test
    void resolveRealIdentity_WhenIdNotFound_ShouldThrowException() {
        UUID unknownId = UUID.randomUUID();
        when(repository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> 
            identityVaultService.resolveRealIdentity(unknownId)
        );
    }
}
