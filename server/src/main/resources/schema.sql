CREATE TABLE IF NOT EXISTS omega_groups (
  group_id TEXT PRIMARY KEY,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CHECK (group_id <> '')
);

CREATE TABLE IF NOT EXISTS devices (
  group_id TEXT NOT NULL REFERENCES omega_groups(group_id) ON DELETE CASCADE,
  device_id INTEGER NOT NULL CHECK (device_id BETWEEN 1 AND 127),
  registration_id INTEGER NOT NULL,
  identity_key BYTEA NOT NULL,
  signed_prekey_id INTEGER NOT NULL,
  signed_prekey BYTEA NOT NULL,
  signed_prekey_signature BYTEA NOT NULL,
  kyber_prekey_id INTEGER NOT NULL,
  kyber_prekey BYTEA NOT NULL,
  kyber_prekey_signature BYTEA NOT NULL,
  auth_token_hash BYTEA NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY(group_id, device_id),
  UNIQUE(group_id, auth_token_hash)
);

CREATE TABLE IF NOT EXISTS invites (
  invite_id UUID PRIMARY KEY,
  group_id TEXT NOT NULL REFERENCES omega_groups(group_id) ON DELETE CASCADE,
  owner_device_id INTEGER NOT NULL,
  token_hash BYTEA NOT NULL UNIQUE,
  expires_at TIMESTAMPTZ NOT NULL,
  consumed_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS messages (
  seq BIGSERIAL PRIMARY KEY,
  group_id TEXT NOT NULL REFERENCES omega_groups(group_id) ON DELETE CASCADE,
  recipient_device_id INTEGER NOT NULL,
  sender_device_id INTEGER NOT NULL,
  idempotency_key TEXT NOT NULL,
  ciphertext BYTEA NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(group_id, recipient_device_id, idempotency_key)
);

CREATE INDEX IF NOT EXISTS messages_recipient_cursor_idx
  ON messages(group_id, recipient_device_id, seq);
