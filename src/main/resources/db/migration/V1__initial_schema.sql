CREATE TABLE IF NOT EXISTS devices (
    device_id UUID PRIMARY KEY,
    platform VARCHAR(10) NOT NULL,
    attested BOOLEAN NOT NULL DEFAULT TRUE,
    attested_at TIMESTAMP,
    tier VARCHAR(20) NOT NULL DEFAULT 'free',
    payment_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    token VARCHAR(255) PRIMARY KEY,
    device_id UUID NOT NULL REFERENCES devices(device_id),
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS readings (
    id UUID PRIMARY KEY,
    device_id UUID NOT NULL REFERENCES devices(device_id),
    spread_type VARCHAR(50) NOT NULL,
    question VARCHAR(500),
    cards TEXT NOT NULL,
    interpretation TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS chat_messages (
    id UUID PRIMARY KEY,
    reading_id UUID NOT NULL REFERENCES readings(id),
    role VARCHAR(10) NOT NULL,
    text TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS daily_usage (
    device_id UUID NOT NULL REFERENCES devices(device_id),
    date DATE NOT NULL,
    readings_count INTEGER NOT NULL DEFAULT 0,
    chat_questions_count INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (device_id, date)
);
