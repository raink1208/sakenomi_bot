-- ギルド設定
CREATE TABLE guild_settings (
    guild_id            BIGINT PRIMARY KEY,
    event_category_id   BIGINT,
    archive_category_id BIGINT,
    admin_role_id       BIGINT,
    log_channel_id      BIGINT,
    recruit_channel_id  BIGINT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ギルドごとの通し番号カウンタ(欠番を許容するため events とは独立)
CREATE TABLE event_counters (
    guild_id     BIGINT PRIMARY KEY,
    next_number  INTEGER NOT NULL DEFAULT 1
);

-- イベント本体
CREATE TABLE events (
    id                 BIGSERIAL PRIMARY KEY,
    guild_id           BIGINT NOT NULL,
    number             INTEGER NOT NULL,           -- 第n回 の n
    name               VARCHAR(80) NOT NULL,
    area               VARCHAR(80) NOT NULL,
    event_date         DATE,                       -- オプション
    status             VARCHAR(16) NOT NULL DEFAULT 'RECRUITING', -- RECRUITING / ARCHIVED
    channel_id         BIGINT NOT NULL,
    role_id            BIGINT NOT NULL,
    recruit_message_id BIGINT,                     -- 募集メッセージ(参加ボタン)の ID
    organizer_user_id  BIGINT NOT NULL,            -- 幹事(作成者)
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    archived_at        TIMESTAMPTZ,
    CONSTRAINT uq_events_guild_number UNIQUE (guild_id, number)
);
CREATE INDEX idx_events_guild_status ON events (guild_id, status);

-- 出欠(イベント単位)
CREATE TABLE event_participants (
    id         BIGSERIAL PRIMARY KEY,
    event_id   BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    user_id    BIGINT NOT NULL,
    joined_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_participant UNIQUE (event_id, user_id)
);

-- 飲み会(お店単位)
CREATE TABLE parties (
    id            BIGSERIAL PRIMARY KEY,
    event_id      BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    order_no      INTEGER NOT NULL,               -- 1軒目=1, 2軒目=2 ...
    shop_name     VARCHAR(120) NOT NULL,
    total_amount  BIGINT,                         -- 会計金額(円)。未確定時 NULL
    note          VARCHAR(255),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_party_order UNIQUE (event_id, order_no)
);

-- 支払受領記録(誰からいくら受け取ったか)
CREATE TABLE payments (
    id            BIGSERIAL PRIMARY KEY,
    party_id      BIGINT NOT NULL REFERENCES parties(id) ON DELETE CASCADE,
    payer_user_id BIGINT NOT NULL,                -- 支払った参加者
    amount        BIGINT NOT NULL,                -- 受領金額(円)
    recorded_by   BIGINT NOT NULL,                -- 記録した幹事/運営
    received_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_payments_party ON payments (party_id);