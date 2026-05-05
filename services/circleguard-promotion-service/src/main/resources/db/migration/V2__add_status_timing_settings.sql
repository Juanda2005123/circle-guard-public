CREATE TABLE IF NOT EXISTS system_settings (
    id SERIAL PRIMARY KEY,
    mandatory_fence_days INTEGER NOT NULL DEFAULT 14,
    encounter_window_days INTEGER NOT NULL DEFAULT 14
);

-- Seed initial values if not present
INSERT INTO system_settings (id, mandatory_fence_days, encounter_window_days) 
VALUES (1, 14, 14) 
ON CONFLICT (id) DO NOTHING;
