-- ゲスト(Discord 未所持・サーバー未参加の参加者。イベント単位の使い捨て)
-- attendances.user_id / payments.payer_user_id には負の疑似 ID(-event_guests.id)として格納する。
-- 削除は論理削除(deleted_at)。同名で再追加した場合は復活させ、出欠・受領記録も元に戻る
CREATE TABLE event_guests (
    id          BIGSERIAL PRIMARY KEY,
    event_id    BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    name        VARCHAR(80) NOT NULL,
    created_by  BIGINT NOT NULL,                 -- 追加した幹事/運営
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ,
    -- 削除済みも含めて一意(同名の再追加は復活として扱うため)
    CONSTRAINT uq_event_guest_name UNIQUE (event_id, name)
);
CREATE INDEX idx_event_guests_event ON event_guests (event_id);
