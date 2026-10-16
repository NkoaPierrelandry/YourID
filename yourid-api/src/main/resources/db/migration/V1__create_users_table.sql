CREATE TABLE users (
    id                UUID PRIMARY KEY,
    email             VARCHAR(255) NOT NULL UNIQUE,
    password          VARCHAR(255),
    first_name        VARCHAR(100) NOT NULL,
    last_name         VARCHAR(100) NOT NULL,
    username          VARCHAR(50) NOT NULL UNIQUE,
    avatar            VARCHAR(500),
    bio               TEXT,
    role              VARCHAR(20) NOT NULL DEFAULT 'SELLER'
                      CHECK (role IN ('ADMIN', 'SELLER', 'BUYER', 'MODERATOR')),
    status            VARCHAR(30) NOT NULL DEFAULT 'PENDING_VERIFICATION'
                      CHECK (status IN ('PENDING_VERIFICATION', 'ACTIVE', 'SUSPENDED', 'INACTIVE', 'BANNED', 'DELETED')),
    email_verified    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
