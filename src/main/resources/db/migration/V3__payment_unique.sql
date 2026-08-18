-- payments: 同一 party_id + payer_user_id の重複行を統合(金額を合算)してから一意制約を追加
-- WITH句(CTE)は1ステートメントの中でのみ有効なため、UPDATE/DELETEそれぞれで定義し直す
WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY party_id, payer_user_id ORDER BY received_at DESC, id DESC) AS rn,
           SUM(amount) OVER (PARTITION BY party_id, payer_user_id) AS total_amount
    FROM payments
)
UPDATE payments p
SET amount = r.total_amount
FROM ranked r
WHERE p.id = r.id AND r.rn = 1;

WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY party_id, payer_user_id ORDER BY received_at DESC, id DESC) AS rn
    FROM payments
)
DELETE FROM payments p
USING ranked r
WHERE p.id = r.id AND r.rn > 1;

ALTER TABLE payments ADD CONSTRAINT uq_payment_party_payer UNIQUE (party_id, payer_user_id);
