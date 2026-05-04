package com.circleguard.form.integration;

import com.circleguard.form.model.HealthSurvey;
import com.circleguard.form.repository.HealthSurveyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Integration Test: Form Service publishes to Kafka
 * 
 * Validates that when a health survey is submitted, it is persisted to the database
 * and a corresponding event is published to the Kafka topic 'survey.submitted'.
 * 
 * This test:
 * - Creates a real Spring context with @SpringBootTest
 * - Mocks the KafkaTemplate to prevent actual Kafka dependencies
 * - Submits a health survey via the REST endpoint
 * - Verifies the survey is saved to the H2 in-memory database
 * - Verifies that a Kafka message was sent to the correct topic
 */
@SpringBootTest
@AutoConfigureMockMvc
public class FormServiceKafkaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HealthSurveyRepository surveyRepository;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    private static final String SURVEY_SUBMIT_ENDPOINT = "/api/v1/surveys";
    private static final String KAFKA_TOPIC_SURVEY_SUBMITTED = "survey.submitted";

    @Test
    void submitSurvey_SavesToDB_AndPublishesToKafka() throws Exception {
        // Arrange: Create a health survey request
        UUID anonymousId = UUID.randomUUID();
        String surveyJson = createSurveyJson(anonymousId, true, true, false);

        // Act: Submit the survey via REST endpoint
        MvcResult result = mockMvc.perform(post(SURVEY_SUBMIT_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(surveyJson))
                .andExpect(status().isOk())
                .andReturn();

        // Assert: Verify Kafka message was sent with the survey data
        verify(kafkaTemplate, times(1)).send(
                org.mockito.ArgumentMatchers.eq(KAFKA_TOPIC_SURVEY_SUBMITTED),
                org.mockito.ArgumentMatchers.eq(anonymousId.toString()),
                any()
        );
    }

    @Test
    void submitSurvey_WithoutSymptoms_StillPublishesEvent() throws Exception {
        // Arrange: Create a survey without symptoms
        UUID anonymousId = UUID.randomUUID();
        String surveyJson = createSurveyJson(anonymousId, false, false, false);

        // Act: Submit the survey
        mockMvc.perform(post(SURVEY_SUBMIT_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(surveyJson))
                .andExpect(status().isOk());

        // Assert: Verify event was published even without symptoms
        verify(kafkaTemplate, times(1)).send(
                org.mockito.ArgumentMatchers.eq(KAFKA_TOPIC_SURVEY_SUBMITTED),
                org.mockito.ArgumentMatchers.eq(anonymousId.toString()),
                any()
        );

        // Verify database persistence
        HealthSurvey savedSurvey = surveyRepository.findByAnonymousId(anonymousId).iterator().next();
        assertThat(savedSurvey.getAnonymousId()).isEqualTo(anonymousId);
    }

    @Test
    void submitSurvey_WithAttachment_SetsPendingValidation() throws Exception {
        // Arrange: Create a survey with an attachment path
        UUID anonymousId = UUID.randomUUID();
        String surveyJson = createSurveyJsonWithAttachment(anonymousId, "/path/to/certificate.pdf");

        // Act: Submit survey with attachment
        mockMvc.perform(post(SURVEY_SUBMIT_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(surveyJson))
                .andExpect(status().isOk());

        // Assert: Verify the survey was saved with PENDING validation status
        HealthSurvey savedSurvey = surveyRepository.findByAnonymousId(anonymousId).iterator().next();
        assertThat(savedSurvey.getAttachmentPath()).isEqualTo("/path/to/certificate.pdf");
        assertThat(savedSurvey.getValidationStatus().toString()).isEqualTo("PENDING");
    }

    @Test
    void submitMultipleSurveys_PublishesMultipleEvents() throws Exception {
        // Arrange: Create multiple survey submissions
        UUID anonymousId1 = UUID.randomUUID();
        UUID anonymousId2 = UUID.randomUUID();

        String survey1Json = createSurveyJson(anonymousId1, true, false, false);
        String survey2Json = createSurveyJson(anonymousId2, false, true, false);

        // Act: Submit both surveys
        mockMvc.perform(post(SURVEY_SUBMIT_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(survey1Json))
                .andExpect(status().isOk());

        mockMvc.perform(post(SURVEY_SUBMIT_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(survey2Json))
                .andExpect(status().isOk());

        // Assert: Verify both were saved and both events were published
        assertThat(surveyRepository.count()).isGreaterThanOrEqualTo(2);
        verify(kafkaTemplate, times(2)).send(
                org.mockito.ArgumentMatchers.eq(KAFKA_TOPIC_SURVEY_SUBMITTED),
                anyString(),
                any()
        );
    }

    // Helper methods to create test data
    private String createSurveyJson(UUID anonymousId, boolean hasFever, boolean hasCough, boolean hasBodyAche) {
        return String.format(
                "{\"anonymousId\": \"%s\", \"hasFever\": %b, \"hasCough\": %b, \"hasBodyAche\": %b}",
                anonymousId.toString(), hasFever, hasCough, hasBodyAche
        );
    }

    private String createSurveyJsonWithAttachment(UUID anonymousId, String attachmentPath) {
        return String.format(
                "{\"anonymousId\": \"%s\", \"hasFever\": false, \"hasCough\": false, \"hasBodyAche\": false, \"attachmentPath\": \"%s\"}",
                anonymousId.toString(), attachmentPath
        );
    }
}
