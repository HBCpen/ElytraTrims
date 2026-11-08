/**
 * Relay for Elytra Trims Cosmetic: stores the latest signed cosmetic of each player and hands them out by UUID.
 *
 * Records are signed by the player's chat signing key, which Mojang certifies for the player's UUID. The relay checks
 * both signatures before storing a record, so nobody can overwrite another player's cosmetic. Clients verify
 * everything again themselves and don't trust the relay.
 *
 * Endpoints:
 *   PUT  /v1/cosmetics/{uuid}   body: a signed record, see verifyRecord()
 *   POST /v1/cosmetics/lookup   body: {"profile_ids": [uuid, ...]}  ->  {"records": [record, ...]}
 */

import { PLAYER_CERTIFICATE_KEYS } from "./mojang-keys.js";

const MOJANG_KEYS_URL = "https://api.minecraftservices.com/publickeys";
const DOMAIN = new TextEncoder().encode("elytratrims_cosmetic/v1\n");

export const LIMITS = {
  recordBytes: 8 * 1024,
  cosmeticLength: 4096,
  lookupBytes: 8 * 1024,
  lookupIds: 100,
  clockSkewMs: 10 * 60 * 1000,
  mojangKeysTtlMs: 60 * 60 * 1000,
};

const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;

export class HttpError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

export default {
  async fetch(request, env) {
    try {
      return await route(request, env);
    } catch (e) {
      if (e instanceof HttpError) return json({ error: e.message }, e.status);
      console.error(e);
      return json({ error: "Internal error" }, 500);
    }
  },

  /** Removes records whose key has expired; they are never served anyway. */
  async scheduled(_event, env) {
    await env.DB.prepare("DELETE FROM cosmetics WHERE expires_at <= ?").bind(Date.now()).run();
  },
};

async function route(request, env) {
  const { pathname } = new URL(request.url);
  if (pathname === "/" && request.method === "GET")
    return new Response("Elytra Trims Cosmetic relay\n", { headers: { "Content-Type": "text/plain" } });

  if (pathname === "/v1/cosmetics/lookup") {
    if (request.method !== "POST") throw new HttpError(405, "Use POST");
    return lookup(request, env);
  }

  const match = pathname.match(/^\/v1\/cosmetics\/([0-9a-fA-F-]{36})$/);
  if (match) {
    if (request.method !== "PUT") throw new HttpError(405, "Use PUT");
    return publish(request, env, match[1].toLowerCase());
  }
  throw new HttpError(404, "Not found");
}

async function publish(request, env, profileId) {
  const record = parseJson(await readBody(request, LIMITS.recordBytes));
  const verified = await verifyRecord(record, profileId, await mojangKeys(env), Date.now());
  // Only a newer record replaces the stored one, so an old record can't be replayed over a newer one.
  await env.DB.prepare(
    `INSERT INTO cosmetics (profile_id, issued_at, expires_at, record) VALUES (?1, ?2, ?3, ?4)
     ON CONFLICT (profile_id) DO UPDATE SET issued_at = ?2, expires_at = ?3, record = ?4 WHERE ?2 > cosmetics.issued_at`,
  ).bind(profileId, verified.issuedAt, verified.expiresAt, JSON.stringify(record)).run();
  return new Response(null, { status: 204 });
}

async function lookup(request, env) {
  const body = parseJson(await readBody(request, LIMITS.lookupBytes));
  const ids = body?.profile_ids;
  if (!Array.isArray(ids) || ids.length > LIMITS.lookupIds) throw new HttpError(400, `profile_ids must be an array of at most ${LIMITS.lookupIds} UUIDs`);
  const unique = [...new Set(ids.map((id) => (typeof id === "string" ? id.toLowerCase() : "")))];
  if (!unique.every((id) => UUID_PATTERN.test(id))) throw new HttpError(400, "Invalid UUID");
  if (unique.length === 0) return json({ records: [] });

  const placeholders = unique.map((_, i) => `?${i + 2}`).join(", ");
  const { results } = await env.DB.prepare(
    `SELECT record FROM cosmetics WHERE expires_at > ?1 AND profile_id IN (${placeholders})`,
  ).bind(Date.now(), ...unique).all();
  return json({ records: results.map((row) => JSON.parse(row.record)) });
}

/**
 * Checks a record and returns its issue and key expiry times (ms):
 * {
 *   "version": 1,
 *   "profile_id": "<uuid>",
 *   "issued_at": <ms since epoch>,
 *   "cosmetic": "<the cosmetic as JSON, or an empty string for none>",
 *   "key": {"expires_at": "<ISO-8601>", "key": "<X.509 in PEM as written by Minecraft>", "signature_v2": "<base64, by Mojang>"},
 *   "signature": "<base64 SHA256withRSA over DOMAIN || uuid || issued_at || cosmetic>"
 * }
 */
export async function verifyRecord(record, profileId, mojangPublicKeys, now) {
  if (typeof record !== "object" || record === null) throw new HttpError(400, "Record must be an object");
  if (record.version !== 1) throw new HttpError(400, "Unsupported version");
  if (record.profile_id !== profileId) throw new HttpError(400, "profile_id doesn't match the URL");
  const issuedAt = record.issued_at;
  if (!Number.isSafeInteger(issuedAt) || issuedAt <= 0) throw new HttpError(400, "Invalid issued_at");
  if (issuedAt > now + LIMITS.clockSkewMs) throw new HttpError(400, "issued_at is in the future");
  if (typeof record.cosmetic !== "string" || record.cosmetic.length > LIMITS.cosmeticLength) throw new HttpError(400, "Invalid cosmetic");
  if (record.cosmetic !== "") {
    let cosmetic;
    try { cosmetic = JSON.parse(record.cosmetic); } catch { throw new HttpError(400, "cosmetic is not JSON"); }
    if (typeof cosmetic !== "object" || cosmetic === null || Array.isArray(cosmetic)) throw new HttpError(400, "cosmetic must be an object");
  }

  const key = record.key;
  if (typeof key !== "object" || key === null) throw new HttpError(400, "Missing key");
  const expiresAt = parseInstant(key.expires_at);
  if (expiresAt <= now) throw new HttpError(400, "Key has expired");
  const keyBytes = pem(key.key, "key.key");
  const keySignature = base64(key.signature_v2, "key.signature_v2");
  const signature = base64(record.signature, "signature");
  const uuid = uuidBytes(profileId);

  // Mojang's certificate: SHA1withRSA over uuid || expires_at (ms) || key (X.509).
  const certified = concat(uuid, long(expiresAt), keyBytes);
  let trusted = false;
  for (const mojangKey of mojangPublicKeys) {
    if (await crypto.subtle.verify("RSASSA-PKCS1-v1_5", mojangKey, keySignature, certified)) {
      trusted = true;
      break;
    }
  }
  if (!trusted) throw new HttpError(403, "Key is not certified by Mojang for this player");

  let playerKey;
  try {
    playerKey = await crypto.subtle.importKey("spki", keyBytes, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["verify"]);
  } catch {
    throw new HttpError(400, "Invalid key");
  }
  const signed = concat(DOMAIN, uuid, long(issuedAt), new TextEncoder().encode(record.cosmetic));
  if (!(await crypto.subtle.verify("RSASSA-PKCS1-v1_5", playerKey, signature, signed))) throw new HttpError(403, "Invalid signature");
  return { issuedAt, expiresAt };
}

/** Mojang's keys for player certificates, cached per isolate. */
let mojangCache = null;

/**
 * Mojang's current keys if they can be fetched, the bundled ones otherwise. Mojang's API answers requests from
 * Cloudflare Workers with 403, so the bundled keys are what is normally used.
 */
async function mojangKeys(env) {
  if (env.MOJANG_KEYS) return env.MOJANG_KEYS; // Tests
  if (mojangCache && mojangCache.fetchedAt + LIMITS.mojangKeysTtlMs > Date.now()) return mojangCache.keys;
  const encoded = [...new Set([...((await fetchMojangKeys()) ?? []), ...PLAYER_CERTIFICATE_KEYS])];
  const keys = (await Promise.all(encoded.map(async (key) => {
    try {
      return await crypto.subtle.importKey("spki", base64(key, "publicKey"), { name: "RSASSA-PKCS1-v1_5", hash: "SHA-1" }, false, ["verify"]);
    } catch {
      return null;
    }
  }))).filter((key) => key !== null);
  mojangCache = { keys, fetchedAt: Date.now() };
  return keys;
}

async function fetchMojangKeys() {
  try {
    const response = await fetch(MOJANG_KEYS_URL, { headers: { Accept: "application/json" } });
    if (!response.ok) {
      console.log(`Using the bundled Mojang keys: ${MOJANG_KEYS_URL} answered ${response.status}`);
      return null;
    }
    const keys = (await response.json()).playerCertificateKeys?.map(({ publicKey }) => publicKey);
    return Array.isArray(keys) && keys.length > 0 && keys.every((key) => typeof key === "string") ? keys : null;
  } catch (e) {
    console.log(`Using the bundled Mojang keys: ${e}`);
    return null;
  }
}

// Helpers

async function readBody(request, limit) {
  const declared = Number(request.headers.get("Content-Length"));
  if (declared > limit) throw new HttpError(413, "Body is too large");
  const body = await request.arrayBuffer();
  if (body.byteLength > limit) throw new HttpError(413, "Body is too large");
  return new TextDecoder().decode(body);
}

function parseJson(text) {
  try {
    return JSON.parse(text);
  } catch {
    throw new HttpError(400, "Body is not JSON");
  }
}

function json(body, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
}

/** Milliseconds of an ISO-8601 instant as written by Java (`2026-11-10T12:00:00.123456Z`), truncated like `toEpochMilli`. */
export function parseInstant(text) {
  const match = typeof text === "string" && text.match(/^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2})(?:\.(\d{1,9}))?Z$/);
  if (!match) throw new HttpError(400, "Invalid key.expires_at");
  const ms = Date.parse(`${match[1]}.${(match[2] ?? "").padEnd(3, "0").slice(0, 3)}Z`);
  if (Number.isNaN(ms)) throw new HttpError(400, "Invalid key.expires_at");
  return ms;
}

function base64(text, field) {
  if (typeof text !== "string" || text.length > 8192) throw new HttpError(400, `Invalid ${field}`);
  try {
    return Uint8Array.from(atob(text), (c) => c.charCodeAt(0));
  } catch {
    throw new HttpError(400, `Invalid ${field}`);
  }
}

/** Minecraft writes X.509 keys with an `RSA PUBLIC KEY` PEM header. */
function pem(text, field) {
  const match = typeof text === "string" && text.match(/^-----BEGIN RSA PUBLIC KEY-----([A-Za-z0-9+/=\s]+)-----END RSA PUBLIC KEY-----\s*$/);
  if (!match) throw new HttpError(400, `Invalid ${field}`);
  return base64(match[1].replace(/\s+/g, ""), field);
}

function uuidBytes(uuid) {
  const hex = uuid.replaceAll("-", "");
  return Uint8Array.from({ length: 16 }, (_, i) => parseInt(hex.slice(i * 2, i * 2 + 2), 16));
}

function long(value) {
  const bytes = new Uint8Array(8);
  new DataView(bytes.buffer).setBigInt64(0, BigInt(value));
  return bytes;
}

function concat(...parts) {
  const result = new Uint8Array(parts.reduce((sum, p) => sum + p.length, 0));
  let offset = 0;
  for (const part of parts) {
    result.set(part, offset);
    offset += part.length;
  }
  return result;
}
