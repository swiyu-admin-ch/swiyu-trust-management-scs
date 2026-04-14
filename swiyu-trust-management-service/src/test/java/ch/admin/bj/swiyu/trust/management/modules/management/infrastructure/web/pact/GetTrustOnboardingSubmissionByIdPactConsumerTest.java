package ch.admin.bj.swiyu.trust.management.modules.management.infrastructure.web.pact;

import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonBody;
import static ch.admin.bj.swiyu.trust.client.core.business.model.TrustOnboardingSubmissionStatusDto.SUBMITTED;
import static ch.admin.bj.swiyu.trust.management.test.pact.PactConsumerSupport.ISO_DATE_TIME_FORMAT_NANOSECONDS;
import static ch.admin.bj.swiyu.trust.management.test.pact.PactConsumerSupport.buildJwsToken;
import static org.assertj.core.api.Assertions.assertThat;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.junit.MockServerConfig;
import au.com.dius.pact.consumer.junit5.PactConsumerTest;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import ch.admin.bit.jeap.security.test.client.MockJeapOAuth2RestClientBuilderFactory;
import ch.admin.bit.jeap.security.test.client.configuration.JeapOAuth2IntegrationTestClientConfiguration;
import ch.admin.bit.jeap.security.test.jws.JwsBuilderFactory;
import ch.admin.bj.swiyu.trust.client.core.business.api.TrustOnboardingSubmissionApi;
import ch.admin.bj.swiyu.trust.client.core.business.invoker.ApiClient;
import ch.admin.bj.swiyu.trust.client.core.business.model.ProofOfPossessionStatusDto;
import ch.admin.bj.swiyu.trust.management.test.pact.PactConsumerTestConfig;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@PactConsumerTest
@PactTestFor(pactVersion = PactSpecVersion.V4)
@MockServerConfig(hostInterface = "localhost", port = "0") // random port
@Import({ JeapOAuth2IntegrationTestClientConfiguration.class, PactConsumerTestConfig.class })
class GetTrustOnboardingSubmissionByIdPactConsumerTest {

    private static final String CONSUMER = "swiyu-trust-management-scs";
    private static final String PROVIDER = "swiyu-core-business-service";

    private static final UUID TRUST_ONBOARDING_SUBMISSION_ID = UUID.randomUUID();
    private static final UUID BUSINESS_PARTNER_ID = UUID.randomUUID();

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private MockJeapOAuth2RestClientBuilderFactory mockRestClientBuilderFactory; // this is used to generate test token which can be stored and reuse later in the provide tests

    private String token;

    @BeforeEach
    void init(@Autowired JwsBuilderFactory jwsBuilderFactory) {
        token = buildJwsToken(
            jwsBuilderFactory,
            "swiyuServiceClient",
            "swiyucorebusiness_@trust", // legacy role, will be removed in future
            "ti_@trustonboardingsubmission_#read"
        );
    }

    @Pact(consumer = CONSUMER, provider = PROVIDER)
    private V4Pact pactGetTrustOnboardingSubmission(PactBuilder builder) {
        return builder
            .given(
                "A submitted TrustOnboardingSubmission for partner ${partnerId} of type BUSINESS exists with the id ${trustOnboardingSubmissionId}",
                Map.of(
                    "partnerId",
                    BUSINESS_PARTNER_ID.toString(),
                    "trustOnboardingSubmissionId",
                    TRUST_ONBOARDING_SUBMISSION_ID.toString()
                )
            )
            .expectsToReceiveHttpInteraction("GET trust onboarding submission by id", httpInteractionBuilder ->
                httpInteractionBuilder
                    .withRequest(httpRequestBuilder ->
                        httpRequestBuilder
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                            .method("GET")
                            .path(
                                "/api/v1/internal/trust/trust-onboarding-submission/" + TRUST_ONBOARDING_SUBMISSION_ID
                            )
                    )
                    .willRespondWith(httpResponseBuilder ->
                        httpResponseBuilder
                            .status(200)
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .body(
                                newJsonBody(o -> {
                                    o.uuid("id", TRUST_ONBOARDING_SUBMISSION_ID);
                                    o.stringValue("id", TRUST_ONBOARDING_SUBMISSION_ID.toString());
                                    o.uuid("partnerId", BUSINESS_PARTNER_ID);
                                    o.stringValue("partnerId", BUSINESS_PARTNER_ID.toString());
                                    o.stringValue("status", "SUBMITTED");
                                    o.object("entityName", e -> {
                                        e.stringType("de", "Firma GmbH");
                                        e.stringType("fr", "Société SàRL");
                                        e.stringType("it", "Società SRL");
                                        e.stringType("en", "Company Ltd");
                                        e.stringType("rm", "Interpresa");
                                    });
                                    o.stringType("entityEmail", "contact@example.com"); // needed both to have the matching capability and the value set for the response to not be null
                                    o.stringMatcher(
                                        "entityEmail",
                                        "^[a-zA-Z0-9.!#$%&'*+=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$",
                                        "contact@example.com"
                                    );
                                    o.object("address", a -> {
                                        a.stringType("street", "Musterstrasse 1");
                                        a.stringType("city", "8000 Zürich");
                                        a.stringType("postalCode", "8000");
                                        a.stringType("country", "CH");
                                    });
                                    o.object("contactPerson", c -> {
                                        c.stringType("firstName", "Max");
                                        c.stringType("lastName", "Mustermann");
                                        c.stringType("email", "m.m@test.ch");
                                        c.stringType("phone", "+78 1234 56 78");
                                        o.object("address", a -> {
                                            a.stringType("street", "Musterstrasse 1");
                                            a.stringType("city", "8000 Zürich");
                                            a.stringType("postalCode", "8000");
                                            a.stringType("country", "CH");
                                        });
                                    });
                                    o.numberType("version", 1);
                                    o.minArrayLike("proofOfPossessions", 1, p -> {
                                        p.stringType("did", "did:swiyu:1234567890");
                                        p.stringType("nonce", "nonce-value");
                                        p.stringValue("status", "VALID");
                                        p.datetime(
                                            "verifiedAt",
                                            ISO_DATE_TIME_FORMAT_NANOSECONDS,
                                            Instant.parse("2024-12-29T09:35:16.000000Z")
                                        );
                                    });
                                    o.stringValue("businessPartnerType", "BUSINESS");
                                    o.stringMatcher("correspondingLanguage", "^(EN|DE|FR|IT|RM)$", "DE");
                                    o.datetime(
                                        "submittedAt",
                                        ISO_DATE_TIME_FORMAT_NANOSECONDS,
                                        Instant.parse("2024-12-29T09:35:16.000000Z")
                                    );
                                    o.datetime(
                                        "createdAt",
                                        ISO_DATE_TIME_FORMAT_NANOSECONDS,
                                        Instant.parse("2024-12-29T09:35:16.000000Z")
                                    );
                                    o.datetime(
                                        "updatedAt",
                                        ISO_DATE_TIME_FORMAT_NANOSECONDS,
                                        Instant.parse("2024-12-29T09:35:16.000000Z")
                                    );
                                }).build()
                            )
                    )
            )
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "pactGetTrustOnboardingSubmission")
    void shouldGetTrustOnboardingSubmissionById(MockServer mockServer) {
        mockRestClientBuilderFactory.getAuthTokenProvider().setAuthToken(token);
        var restClient = mockRestClientBuilderFactory.createForTokenFromIncomingRequest().build();

        var apiClient = new ApiClient(restClient);
        apiClient.setBearerToken(token);
        var api = new TrustOnboardingSubmissionApi(apiClient);
        api.getApiClient().setBasePath(mockServer.getUrl());
        var submission = api.getTrustOnboardingSubmission(TRUST_ONBOARDING_SUBMISSION_ID);

        assertThat(submission).isNotNull();
        assertThat(submission.getId()).isEqualTo(TRUST_ONBOARDING_SUBMISSION_ID);
        assertThat(submission.getPartnerId()).isEqualTo(BUSINESS_PARTNER_ID);
        assertThat(submission.getStatus()).isEqualTo(SUBMITTED);
        assertThat(submission.getProofOfPossessions()).isNotEmpty();
        assertThat(submission.getProofOfPossessions()).allMatch(p ->
            ProofOfPossessionStatusDto.VALID.equals(p.getStatus())
        );
    }
}
