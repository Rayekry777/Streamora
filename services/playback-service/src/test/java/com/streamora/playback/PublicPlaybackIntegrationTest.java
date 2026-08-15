package com.streamora.playback;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PublicPlaybackIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnPublicHlsManifestWithExpiration() throws Exception {
        mockMvc.perform(get("/api/v1/videos/city-pet-journey/playback").header("X-Request-Id", "playback-test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("playback-test"))
                .andExpect(jsonPath("$.data.videoId").value("city-pet-journey"))
                .andExpect(jsonPath("$.data.manifestUrl").value("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"))
                .andExpect(jsonPath("$.data.expiresAt").exists());
    }

    @Test
    void shouldRejectUnavailablePlayback() throws Exception {
        mockMvc.perform(get("/api/v1/videos/not-public/playback"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PLAYBACK_NOT_FOUND"));
    }
}
