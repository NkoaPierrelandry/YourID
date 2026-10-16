CREATE TABLE payment_accounts (
    id           UUID PRIMARY KEY,
    store_id     UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    method       VARCHAR(30) NOT NULL
                 CHECK (method IN ('ORANGE_MONEY', 'WAVE', 'MTN_MONEY', 'BANK_TRANSFER')),
    label        VARCHAR(100),
    account_info JSONB NOT NULL,
    is_default   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_payment_accounts_store ON payment_accounts(store_id);

ALTER TABLE withdrawals ADD COLUMN payment_account_id UUID REFERENCES payment_accounts(id) ON DELETE SET NULL;