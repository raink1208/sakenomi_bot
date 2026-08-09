-- 出欠記録(飲み会単位。event_participants とは別に「実際に来たか」を管理する)
CREATE TABLE attendances (
    id            BIGSERIAL PRIMARY KEY,
    party_id      BIGINT NOT NULL REFERENCES parties(id) ON DELETE CASCADE,
    user_id       BIGINT NOT NULL,
    status        VARCHAR(16) NOT NULL,          -- PRESENT / ABSENT
    recorded_by   BIGINT NOT NULL,               -- 記録した幹事/運営
    recorded_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_attendance UNIQUE (party_id, user_id)
);
CREATE INDEX idx_attendances_party ON attendances (party_id);
