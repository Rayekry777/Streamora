package com.streamora.pet.application;

import com.streamora.pet.domain.ActivePet;
import com.streamora.pet.identity.UserIdentityClient;
import com.streamora.pet.infrastructure.PetInstanceRepository;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Selects the public mascot or the user's persistent personal pet. */
@Service
public class ActivePetService {

    private final UserIdentityClient identityClient;
    private final PetInstanceRepository repository;
    private final String publicDisplayName;
    private final String publicAssetKey;
    private final String personalAssetKey;
    private final Clock clock = Clock.systemUTC();

    public ActivePetService(
            UserIdentityClient identityClient,
            PetInstanceRepository repository,
            @Value("${streamora.pet.public-display-name}") String publicDisplayName,
            @Value("${streamora.pet.public-asset-key}") String publicAssetKey,
            @Value("${streamora.pet.personal-asset-key}") String personalAssetKey) {
        this.identityClient = identityClient;
        this.repository = repository;
        this.publicDisplayName = publicDisplayName;
        this.publicAssetKey = publicAssetKey;
        this.personalAssetKey = personalAssetKey;
    }

    @Transactional
    public ActivePet resolve(String rawUserToken, String traceId) {
        return identityClient.resolveUser(rawUserToken, traceId)
                .map(principal -> repository.findOrCreate(
                        principal.subjectId(), principal.displayName(), personalAssetKey, clock.instant()))
                .orElseGet(this::publicMascot);
    }

    private ActivePet publicMascot() {
        return new ActivePet("public-mascot", publicDisplayName, publicAssetKey, "PUBLIC", null);
    }
}
