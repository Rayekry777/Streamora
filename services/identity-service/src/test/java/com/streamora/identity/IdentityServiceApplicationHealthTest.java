package com.streamora.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies that the service starts and exposes its liveness probe without infrastructure.
 */
@SpringBootTest
@AutoConfigureMockMvc
class IdentityServiceApplicationHealthTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Confirms that the local profile exposes an UP liveness response.
     *
     * @throws Exception when the HTTP probe cannot be executed
     */
    @Test
    void shouldExposeLivenessHealth() throws Exception {
        var response = mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        assertThat(response.getContentAsString()).contains("\"status\":\"UP\"");
    }
}
