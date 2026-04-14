package ch.admin.bj.swiyu.trust.management.modules.ui.infrastructure.web.security;

import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static org.springframework.http.HttpHeaders.ORIGIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.bj.swiyu.trust.management.test.PostgreSQLContainerInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles("test")
@SpringBootTest(properties = "cors.allowed-origins=http://allowed.example.com")
@AutoConfigureMockMvc
@Testcontainers
@ContextConfiguration(initializers = PostgreSQLContainerInitializer.class)
class CorsIT {

    private static final String ALLOWED_ORIGIN = "http://allowed.example.com";
    private static final String DISALLOWED_ORIGIN = "http://evil.example.com";
    private static final String TEST_ENDPOINT = "/ui-api/configuration/";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void preflight_allowedOrigin_returnsCorsHeaders() throws Exception {
        mockMvc
            .perform(
                options(TEST_ENDPOINT).header(ORIGIN, ALLOWED_ORIGIN).header("Access-Control-Request-Method", "GET")
            )
            .andExpect(status().isOk())
            .andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN));
    }

    @Test
    void preflight_disallowedOrigin_doesNotReturnCorsHeaders() throws Exception {
        mockMvc
            .perform(
                options(TEST_ENDPOINT).header(ORIGIN, DISALLOWED_ORIGIN).header("Access-Control-Request-Method", "GET")
            )
            .andExpect(header().doesNotExist(ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void simpleRequest_allowedOrigin_returnsCorsHeader() throws Exception {
        mockMvc
            .perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(TEST_ENDPOINT).header(
                    ORIGIN,
                    ALLOWED_ORIGIN
                )
            )
            .andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN));
    }

    @Test
    void simpleRequest_disallowedOrigin_doesNotReturnCorsHeader() throws Exception {
        mockMvc
            .perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(TEST_ENDPOINT).header(
                    ORIGIN,
                    DISALLOWED_ORIGIN
                )
            )
            .andExpect(header().doesNotExist(ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
