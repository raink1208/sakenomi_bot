-- 推しロール(サーバーごとに運営が手動作成したロールと推しの名前を紐付ける)
-- ロール自体の作成・削除は Discord 側で手動で行い、Bot は付与・剥奪のみを行う
CREATE TABLE oshi_roles (
    id          BIGSERIAL PRIMARY KEY,
    guild_id    BIGINT NOT NULL,
    role_id     BIGINT NOT NULL,
    name        VARCHAR(80) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_oshi_roles_role UNIQUE (guild_id, role_id),
    CONSTRAINT uq_oshi_roles_name UNIQUE (guild_id, name)
);
CREATE INDEX idx_oshi_roles_guild ON oshi_roles (guild_id);
