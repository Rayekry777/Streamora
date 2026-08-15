package com.streamora.pet.api;

import com.streamora.pet.application.ActivePetService;
import com.streamora.pet.domain.ActivePet;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public pet selector; only the user cookie can activate a personal pet. */
@RestController
@RequestMapping("/api/v1/pets")
public class ActivePetController {

    private final ActivePetService activePetService;

    public ActivePetController(ActivePetService activePetService) {
        this.activePetService = activePetService;
    }

    @GetMapping("/active")
    public ApiEnvelope<ActivePet> activePet(
            @CookieValue(value = "STREAMORA_USER_SESSION", required = false) String rawUserToken,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        String traceId = RequestIds.resolve(requestId);
        return new ApiEnvelope<>(activePetService.resolve(rawUserToken, traceId), traceId);
    }
}
