ALTER TABLE person_profiles
    ADD COLUMN user_id BIGINT UNSIGNED NULL AFTER id,
    ADD UNIQUE KEY uq_person_profile_user (user_id),
    ADD CONSTRAINT fk_pp_user FOREIGN KEY (user_id) REFERENCES users (id);