package com.streamora.video;

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
class PublicVideoQueryIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnPublishedPublicHomeFeedAndPreserveRequestId() throws Exception {
        mockMvc.perform(get("/api/v1/home/feed?category=life").header("X-Request-Id", "video-read-test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("video-read-test"))
                .andExpect(jsonPath("$.data.featuredVideo.videoId").value("city-pet-journey"))
                .andExpect(jsonPath("$.data.items[0].category").value("生活"))
                .andExpect(jsonPath("$.data.categories[0].categoryId").value("all"));
    }

    @Test
    void shouldReturnPublicVideoDetailAndHideUnknownVideo() throws Exception {
        mockMvc.perform(get("/api/v1/videos/city-pet-journey"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.videoId").value("city-pet-journey"))
                .andExpect(jsonPath("$.data.tags[0]").exists())
                .andExpect(jsonPath("$.data.episodes[0].isCurrent").value(true));

        mockMvc.perform(get("/api/v1/videos/not-public"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("VIDEO_NOT_FOUND"));
    }
}
