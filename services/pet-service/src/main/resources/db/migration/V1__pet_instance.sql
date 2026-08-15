CREATE SCHEMA IF NOT EXISTS pet;

CREATE TABLE pet.pet_instance (
    id VARCHAR(36) PRIMARY KEY,
    owner_subject_id VARCHAR(40) NOT NULL UNIQUE,
    display_name VARCHAR(80) NOT NULL,
    asset_key VARCHAR(240) NOT NULL,
    status VARCHAR(24) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_pet_instance_owner_status
    ON pet.pet_instance(owner_subject_id, status);
