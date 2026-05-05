package com.circleguard.gateway.e2e;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * System-Wide End-to-End Tests for Circle Guard.
 * 
 * Assumes real containers are running via docker-compose (postgres, neo4j, kafka, redis, ldap, 
 * and all 6 microservices).
 * 
 * Flow covers:
 * 1. Autenticación
 * 2. Registro de Perfil
 * 3. Envio de Formulario de Salud
 * 4. Interacción / Aplicación de Promoción
 * 5. Seguridad (Validación en el Gateway)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SystemWideE2ETest {

    private static final String GATEWAY_URL = "http://localhost:8087";
    // Si el Gateway an no tiene configuradas las rutas inversas a los microservicios, 
    // asumimos que el ingress de localhost:8087 rutea /api/v1/auth hacia el auth-service, etc.
    // De lo contrario, estas pruebas verificarn que el Gateway cumpla su definicin.

    private static String jwtToken;
    private static String anonymousId;

    @BeforeAll
    static void setup() throws InterruptedException {
        RestAssured.baseURI = GATEWAY_URL;

        // Health Check Waiter: Wait for Gateway to be up before starting E2E flows
        System.out.println("⏳ Esperando a que el Gateway (y los servicios backends) estén disponibles en " + GATEWAY_URL + "...");
        boolean isUp = false;
        int retries = 15;
        
        while (!isUp && retries > 0) {
            try {
                // Hacer un ping a un endpoint publico o base del gateway
                // Si devuelve algun status HTTP (como 404 o 401), el servicio está arriba
                Response resp = given().baseUri(GATEWAY_URL).when().get("/actuator/health");
                isUp = true;
                System.out.println("✅ Gateway detectado. Iniciando pruebas E2E.");
            } catch (Exception e) {
                System.out.println("Gateway no disponible aún. Reintentando en 3s... (Intentos restantes: " + retries + ")");
                retries--;
                Thread.sleep(3000);
            }
        }
        
        if (!isUp) {
            System.err.println("❌ Timeout esperando al Gateway. Las pruebas dudosamente pasaran.");
        }
    }

    @Test
    @Order(1)
    void flow1_Authentication_ReturnsValidJwt() {
        String loginPayload = """
                {
                    "username": "super_admin",
                    "password": "password"
                }
                """;

        Response response = given()
            .log().all()
            .baseUri(GATEWAY_URL)
            .contentType(ContentType.JSON)
            .body(loginPayload)
        .when()
            .post("/api/v1/auth/login");
        
        response.then()
                .log().all()
                .statusCode(is(200))
                .body("token", notNullValue())
                .body("anonymousId", notNullValue());

        jwtToken = response.jsonPath().getString("token");
        anonymousId = response.jsonPath().getString("anonymousId");
    }

    @Test
    @Order(2)
    void flow2_IdentityProfile_RegistrationAndQuery() {
        String mockRealIdentity = "student@example.edu";
        
        String mapPayload = String.format("""
                {
                    "realIdentity": "%s"
                }
                """, mockRealIdentity);

        given()
            .baseUri(GATEWAY_URL)
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + jwtToken)
            .body(mapPayload)
        .when()
            .post("/api/v1/identities/map")
        .then()
            .statusCode(anyOf(is(200), is(201)));
    }

    @Test
    @Order(3)
    void flow3_HealthSurvey_SubmissionAndAsyncNotification() {
        String headerId = anonymousId != null ? anonymousId : UUID.randomUUID().toString();
        
        String surveyPayload = String.format("""
                {
                    "anonymousId": "%s",
                    "symptoms": ["FEVER", "COUGH"],
                    "temperature": 38.5,
                    "contactWithInfected": true
                }
                """, headerId);

        given()
            .baseUri(GATEWAY_URL)
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + jwtToken)
            .header("X-Anonymous-Id", headerId)
            .body(surveyPayload)
        .when()
            .post("/api/v1/surveys")
        .then()
            .statusCode(anyOf(is(200), is(201), is(202)));
    }

    @Test
    @Order(4)
    void flow4_PromotionAndNetworking_Neo4jGraph() {
        String peerId = UUID.randomUUID().toString();
        String headerId = anonymousId != null ? anonymousId : UUID.randomUUID().toString();
        
        String encounterPayload = String.format("""
                {
                    "sourceId": "%s",
                    "targetId": "%s",
                    "locationId": "building-123"
                }
                """, headerId, peerId);

        given()
            .baseUri(GATEWAY_URL)
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + jwtToken)
            .body(encounterPayload)
        .when()
            .post("/api/v1/encounters/report")
        .then()
            .statusCode(anyOf(is(200), is(201)));
    }

    @Test
    @Order(5)
    void flow5_GatewaySecurity_AccessDeniedWithoutToken() {
        // Peticion directa al entrypoint de validacion o de acceso seguro
        // Usamos el unico path verificado en el app de GATEWAY: POST /api/v1/gate/validate
        String invalidRequest = """
                {
                    "token": "esto_no_es_un_jwt_y_es_invalido"
                }
                """;

        given()
            .baseUri(GATEWAY_URL)
            .contentType(ContentType.JSON)
            .body(invalidRequest)
        .when()
            .post("/api/v1/gate/validate")
        .then()
            .statusCode(401);
            
        given()
            .baseUri(GATEWAY_URL)
            .contentType(ContentType.JSON)
            .body("{}")
        .when()
            .post("/api/v1/gate/validate")
        .then()
            .statusCode(401);
    }
}
