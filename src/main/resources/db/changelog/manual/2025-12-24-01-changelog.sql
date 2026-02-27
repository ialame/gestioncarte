-- liquibase formatted sql

-- changeset r.alhajjaj:2025-12-24-add-id-prim-zh-history
ALTER TABLE j_hbn_history__pokemon_card
    ADD COLUMN IF NOT EXISTS id_prim_zh VARCHAR(255) NULL COMMENT 'Primary Id in Chinese (Simplified)';