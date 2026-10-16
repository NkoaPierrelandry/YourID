-- ============================================================
-- Dependent uniquement de users (deja creee en V1)
-- ============================================================

CREATE TABLE audit_logs (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    action      VARCHAR(100) NOT NULL,
    entity      VARCHAR(100),
    entity_id   UUID,
    ip_address  VARCHAR(45),
    user_agent  VARCHAR(500),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE password_reset_tokens (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token      VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE notifications (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type       VARCHAR(50) NOT NULL,
    title      VARCHAR(255) NOT NULL,
    message    TEXT NOT NULL,
    data       JSONB,
    is_read    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_notifications_user ON notifications(user_id, is_read);

CREATE TABLE ai_contents (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type       VARCHAR(50) NOT NULL,
    prompt     TEXT NOT NULL,
    result     TEXT NOT NULL,
    model      VARCHAR(100) NOT NULL,
    tokens     INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- Aucune dependance (tables autonomes)
-- ============================================================

CREATE TABLE categories (
    id         UUID PRIMARY KEY,
    name       VARCHAR(100) NOT NULL UNIQUE,
    slug       VARCHAR(100) NOT NULL UNIQUE,
    icon       VARCHAR(100),
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE email_logs (
    id         UUID PRIMARY KEY,
    recipient  VARCHAR(255) NOT NULL,
    subject    VARCHAR(255) NOT NULL,
    template   VARCHAR(100) NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'sent',
    error      TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE analytics_events (
    id         UUID PRIMARY KEY,
    store_id   UUID,
    product_id UUID,
    event      VARCHAR(100) NOT NULL,
    properties JSONB,
    ip_address VARCHAR(45),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- stores : depend de users
-- ============================================================

CREATE TABLE stores (
    id             UUID PRIMARY KEY,
    user_id        UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name           VARCHAR(150) NOT NULL,
    slug           VARCHAR(150) NOT NULL UNIQUE,
    description    TEXT,
    logo           VARCHAR(500),
    banner         VARCHAR(500),
    primary_color  VARCHAR(7) NOT NULL DEFAULT '#00A86B',
    currency       VARCHAR(3) NOT NULL DEFAULT 'XOF',
    country        VARCHAR(2) NOT NULL DEFAULT 'SN',
    website        VARCHAR(255),
    twitter        VARCHAR(255),
    instagram      VARCHAR(255),
    youtube        VARCHAR(255),
    tiktok         VARCHAR(255),
    facebook       VARCHAR(255),
    is_verified    BOOLEAN NOT NULL DEFAULT FALSE,
    total_revenue  NUMERIC(14,2) NOT NULL DEFAULT 0,
    total_sales    INTEGER NOT NULL DEFAULT 0,
    balance        NUMERIC(14,2) NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- products : depend de stores, categories
-- ============================================================

CREATE TABLE products (
    id             UUID PRIMARY KEY,
    store_id       UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    category_id    UUID REFERENCES categories(id),
    type           VARCHAR(20) NOT NULL DEFAULT 'EBOOK'
                   CHECK (type IN ('EBOOK','COURSE','AUDIO','TEMPLATE','SOFTWARE','SERVICE','SUBSCRIPTION','OTHER')),
    status         VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                   CHECK (status IN ('DRAFT','PUBLISHED','PRIVATE')),
    title          VARCHAR(255) NOT NULL,
    slug           VARCHAR(255) NOT NULL UNIQUE,
    description    TEXT,
    short_desc     VARCHAR(500),
    cover_image    VARCHAR(500),
    price          NUMERIC(12,2) NOT NULL,
    compare_price  NUMERIC(12,2),
    currency       VARCHAR(3) NOT NULL DEFAULT 'XOF',
    content_url    VARCHAR(1000),
    content_type   VARCHAR(20)
                   CHECK (content_type IN ('YOUTUBE','VIMEO','GDRIVE','DROPBOX','ICLOUD','URL')),
    content_note   TEXT,
    is_featured    BOOLEAN NOT NULL DEFAULT FALSE,
    is_marketplace BOOLEAN NOT NULL DEFAULT TRUE,
    total_sales    INTEGER NOT NULL DEFAULT 0,
    total_revenue  NUMERIC(14,2) NOT NULL DEFAULT 0,
    rating         NUMERIC(3,2) NOT NULL DEFAULT 0,
    review_count   INTEGER NOT NULL DEFAULT 0,
    view_count     INTEGER NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_products_store_status ON products(store_id, status);
CREATE INDEX idx_products_marketplace ON products(status, is_marketplace);
CREATE INDEX idx_products_sales ON products(total_sales);

-- ============================================================
-- customers : depend de stores
-- ============================================================

CREATE TABLE customers (
    id          UUID PRIMARY KEY,
    store_id    UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    email       VARCHAR(255) NOT NULL,
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100) NOT NULL,
    total_spent NUMERIC(14,2) NOT NULL DEFAULT 0,
    order_count INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (store_id, email)
);

-- ============================================================
-- orders : depend de stores, customers
-- ============================================================

CREATE TABLE orders (
    id             UUID PRIMARY KEY,
    store_id       UUID NOT NULL REFERENCES stores(id),
    customer_id    UUID REFERENCES customers(id),
    order_number   VARCHAR(50) NOT NULL UNIQUE,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                   CHECK (status IN ('PENDING','COMPLETED','REFUNDED')),
    subtotal       NUMERIC(12,2) NOT NULL,
    platform_fee   NUMERIC(12,2) NOT NULL,
    seller_revenue NUMERIC(12,2) NOT NULL,
    total          NUMERIC(12,2) NOT NULL,
    currency       VARCHAR(3) NOT NULL DEFAULT 'XOF',
    customer_email VARCHAR(255) NOT NULL,
    customer_name  VARCHAR(255) NOT NULL,
    payment_method VARCHAR(50),
    payment_ref    VARCHAR(255),
    completed_at   TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_orders_store ON orders(store_id, status, created_at);
CREATE INDEX idx_orders_email ON orders(customer_email);

-- ============================================================
-- order_items : depend de orders, products
-- ============================================================

CREATE TABLE order_items (
    id         UUID PRIMARY KEY,
    order_id   UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id),
    title      VARCHAR(255) NOT NULL,
    price      NUMERIC(12,2) NOT NULL,
    quantity   INTEGER NOT NULL DEFAULT 1
);

-- ============================================================
-- transactions : depend de orders
-- ============================================================

CREATE TABLE transactions (
    id           UUID PRIMARY KEY,
    order_id     UUID UNIQUE NOT NULL REFERENCES orders(id),
    amount       NUMERIC(12,2) NOT NULL,
    currency     VARCHAR(3) NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                 CHECK (status IN ('PENDING','COMPLETED')),
    provider     VARCHAR(50) NOT NULL,
    provider_ref VARCHAR(255),
    processed_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- withdrawals : depend de stores
-- ============================================================

CREATE TABLE withdrawals (
    id           UUID PRIMARY KEY,
    store_id     UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    amount       NUMERIC(12,2) NOT NULL,
    currency     VARCHAR(3) NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                 CHECK (status IN ('PENDING','APPROVED','PAID','REJECTED')),
    method       VARCHAR(50) NOT NULL,
    account_info JSONB NOT NULL,
    notes        TEXT,
    admin_note   TEXT,
    processed_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_withdrawals_store ON withdrawals(store_id, status);

-- ============================================================
-- reviews : depend de stores, products
-- ============================================================

CREATE TABLE reviews (
    id          UUID PRIMARY KEY,
    store_id    UUID NOT NULL REFERENCES stores(id),
    product_id  UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    rating      INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    title       VARCHAR(255),
    comment     TEXT,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
