-- The latest signed cosmetic of each player.
CREATE TABLE IF NOT EXISTS cosmetics (
  profile_id TEXT PRIMARY KEY,
  -- When the record was signed (ms since epoch); only newer records replace a stored one.
  issued_at INTEGER NOT NULL,
  -- When the signing key expires (ms since epoch); expired records are no longer served.
  expires_at INTEGER NOT NULL,
  record TEXT NOT NULL
);
