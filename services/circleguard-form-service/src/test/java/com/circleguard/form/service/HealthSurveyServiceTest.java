package com.circleguard.form.service;

import com.circleguard.form.model.HealthSurvey;
import com.circleguard.form.model.Questionnaire;
import com.circleguard.form.model.ValidationStatus;
import com.circleguard.form.repository.HealthSurveyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthSurveyServiceTest {

    @Mock
    private HealthSurveyRepository repository;

    @Mock
    private QuestionnaireService questionnaireService;

    @Mock
    private SymptomMapper symptomMapper;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private HealthSurveyService healthSurveyService;

    private HealthSurvey mockSurvey;
    private final UUID anonymousId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockSurvey = new HealthSurvey();
        mockSurvey.setId(UUID.randomUUID());
        mockSurvey.setAnonymousId(anonymousId);
    }

    @Test
    void submitSurvey_WithSymptoms_ShouldSaveAndEmitEvent() {
        Questionnaire q = new Questionnaire();
        when(questionnaireService.getActiveQuestionnaire()).thenReturn(Optional.of(q));
        when(symptomMapper.hasSymptoms(any(HealthSurvey.class), eq(q))).thenReturn(true);
        when(repository.save(any(HealthSurvey.class))).thenReturn(mockSurvey);

        HealthSurvey saved = healthSurveyService.submitSurvey(mockSurvey);

        assertTrue(saved.getHasFever());
        assertTrue(saved.getHasCough());
        verify(repository, times(1)).save(mockSurvey);
        verify(kafkaTemplate, times(1)).send(eq("survey.submitted"), eq(anonymousId.toString()), anyMap());
    }

    @Test
    void validateSurvey_WhenApproved_ShouldUpdateAndEmitEvent() {
        UUID adminId = UUID.randomUUID();
        when(repository.findById(mockSurvey.getId())).thenReturn(Optional.of(mockSurvey));

        healthSurveyService.validateSurvey(mockSurvey.getId(), ValidationStatus.APPROVED, adminId);

        assertEquals(ValidationStatus.APPROVED, mockSurvey.getValidationStatus());
        assertEquals(adminId, mockSurvey.getValidatedBy());
        verify(repository, times(1)).save(mockSurvey);
        verify(kafkaTemplate, times(1)).send(eq("certificate.validated"), eq(anonymousId.toString()), anyMap());
    }

    @Test
    void validateSurvey_WhenSurveyNotFound_ShouldThrowException() {
        when(repository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> 
            healthSurveyService.validateSurvey(UUID.randomUUID(), ValidationStatus.APPROVED, UUID.randomUUID())
        );
    }
}
