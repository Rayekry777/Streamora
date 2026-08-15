package com.streamora.pet.domain;

/** Renderable pet identity selected for the current browser session. */
public record ActivePet(
        String petId,
        String displayName,
        String assetKey,
        String source,
        String ownerSubjectId) {
}
