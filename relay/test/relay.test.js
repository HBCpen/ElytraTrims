// Run with `npm test` (Node 22.5 or newer, for node:sqlite).
// Set ET_SYNC_SAMPLE to a sample written by the client game test to also check records signed by the mod itself.
import { DatabaseSync } from "node:sqlite";
import { generateKeyPairSync, sign } from "node:crypto";
import { readFileSync } from "node:fs";
import { test } from "node:test";
import assert from "node:assert/strict";
import worker, { verifyRecord } from "../src/index.js";

const SCHEMA = readFileSync(new URL("../schema.sql", import.meta.url), "utf8");
const DOMAIN = Buffer.from("elytratrims_cosmetic/v1\n");
const HOUR = 60 * 60 * 1000;

/** The subset of the D1 API used by the relay, backed by SQLite like D1 itself. */
class FakeD1 {
  constructor() {
    this.db = new DatabaseSync(":memory:");
    this.db.exec(SCHEMA);
  }

  prepare(sql) {
    const statement = this.db.prepare(sql);
    return {
      bind: (...args) => ({
        run: async () => (statement.run(...args), { success: true }),
        all: async () => ({ results: statement.all(...args) }),
      }),
    };
  }
}

const mojang = generateKeyPairSync("rsa", { modulusLength: 2048 });
const mojangKey = await crypto.subtle.importKey(
  "spki", mojang.publicKey.export({ type: "spki", format: "der" }), { name: "RSASSA-PKCS1-v1_5", hash: "SHA-1" }, false, ["verify"]);

function uuidBytes(uuid) {
  return Buffer.from(uuid.replaceAll("-", ""), "hex");
}

function long(value) {
  const buffer = Buffer.alloc(8);
  buffer.writeBigInt64BE(BigInt(value));
  return buffer;
}

/** Like Crypt.rsaPublicKeyToString: X.509 in base64 with MIME line breaks, under an "RSA PUBLIC KEY" header. */
function minecraftPem(der) {
  const lines = der.toString("base64").match(/.{1,76}/g).join("\n");
  return `-----BEGIN RSA PUBLIC KEY-----\n${lines}\n-----END RSA PUBLIC KEY-----\n`;
}

/** A player key certified by the fake Mojang key, the way Mojang certifies chat keys. */
function playerKey(profileId, expiresAt = Date.now() + 48 * HOUR) {
  const pair = generateKeyPairSync("rsa", { modulusLength: 2048 });
  const der = pair.publicKey.export({ type: "spki", format: "der" });
  const certificate = sign("sha1", Buffer.concat([uuidBytes(profileId), long(expiresAt), der]), mojang.privateKey);
  return {
    privateKey: pair.privateKey,
    json: { expires_at: new Date(expiresAt).toISOString(), key: minecraftPem(der), signature_v2: certificate.toString("base64") },
  };
}

function record(profileId, key, cosmetic, issuedAt = Date.now()) {
  const signed = Buffer.concat([DOMAIN, uuidBytes(profileId), long(issuedAt), Buffer.from(cosmetic, "utf8")]);
  return {
    version: 1,
    profile_id: profileId,
    issued_at: issuedAt,
    cosmetic,
    key: key.json,
    signature: sign("sha256", signed, key.privateKey).toString("base64"),
  };
}

function relay() {
  const env = { DB: new FakeD1(), MOJANG_KEYS: [mojangKey] };
  const call = (method, path, body) => worker.fetch(new Request(`https://relay.test${path}`, {
    method, body: body === undefined ? undefined : typeof body === "string" ? body : JSON.stringify(body),
  }), env);
  return {
    env,
    put: (id, body) => call("PUT", `/v1/cosmetics/${id}`, body),
    lookup: async (ids) => {
      const response = await call("POST", "/v1/cosmetics/lookup", { profile_ids: ids });
      assert.equal(response.status, 200);
      return (await response.json()).records;
    },
    call,
  };
}

const ALEX = "6ab43178-89fd-4905-97f6-0f67d9d76fd9";
const STEVE = "8667ba71-b85a-4004-af54-457a9734eed7";
const COSMETIC = JSON.stringify({ trim: { pattern: "minecraft:wild", material: "minecraft:gold" }, glow: true });

test("stores a valid record and hands it out", async () => {
  const r = relay();
  const key = playerKey(ALEX);
  const published = record(ALEX, key, COSMETIC);
  assert.equal((await r.put(ALEX, published)).status, 204);
  assert.deepEqual(await r.lookup([ALEX, STEVE]), [published]);
  // Upper case UUIDs are the same player.
  assert.deepEqual(await r.lookup([ALEX.toUpperCase()]), [published]);
});

test("keeps the newest record", async () => {
  const r = relay();
  const key = playerKey(ALEX);
  const now = Date.now();
  const newer = record(ALEX, key, COSMETIC, now);
  const older = record(ALEX, key, "", now - 1000);
  assert.equal((await r.put(ALEX, newer)).status, 204);
  assert.equal((await r.put(ALEX, older)).status, 204);
  assert.deepEqual(await r.lookup([ALEX]), [newer]);

  const cleared = record(ALEX, key, "", now + 1000);
  assert.equal((await r.put(ALEX, cleared)).status, 204);
  assert.deepEqual(await r.lookup([ALEX]), [cleared]);
});

test("rejects records that weren't signed by the player", async () => {
  const r = relay();
  const key = playerKey(ALEX);

  const tampered = { ...record(ALEX, key, COSMETIC), cosmetic: JSON.stringify({ glow: true }) };
  assert.equal((await r.put(ALEX, tampered)).status, 403);

  // Alex's certified key can't sign for Steve.
  assert.equal((await r.put(STEVE, record(STEVE, key, COSMETIC))).status, 403);

  // A key that Mojang didn't certify.
  const forged = playerKey(ALEX);
  const otherMojang = generateKeyPairSync("rsa", { modulusLength: 2048 });
  const der = Buffer.from(forged.json.key.split("\n").slice(1, -2).join(""), "base64");
  forged.json.signature_v2 = sign("sha1", Buffer.concat([uuidBytes(ALEX), long(Date.parse(forged.json.expires_at)), der]), otherMojang.privateKey).toString("base64");
  assert.equal((await r.put(ALEX, record(ALEX, forged, COSMETIC))).status, 403);

  // The URL and the record must name the same player.
  assert.equal((await r.put(STEVE, record(ALEX, key, COSMETIC))).status, 400);
  assert.deepEqual(await r.lookup([ALEX, STEVE]), []);
});

test("rejects expired keys, future records and malformed input", async () => {
  const r = relay();
  assert.equal((await r.put(ALEX, record(ALEX, playerKey(ALEX, Date.now() - 1000), COSMETIC))).status, 400);
  assert.equal((await r.put(ALEX, record(ALEX, playerKey(ALEX), COSMETIC, Date.now() + 2 * HOUR))).status, 400);
  assert.equal((await r.put(ALEX, record(ALEX, playerKey(ALEX), "not json"))).status, 400);
  assert.equal((await r.put(ALEX, record(ALEX, playerKey(ALEX), "x".repeat(5000)))).status, 400);
  assert.equal((await r.put(ALEX, "{")).status, 400);
  assert.equal((await r.put(ALEX, "x".repeat(9000))).status, 413);
  assert.equal((await r.call("POST", "/v1/cosmetics/lookup", { profile_ids: ["nope"] })).status, 400);
  assert.equal((await r.call("POST", "/v1/cosmetics/lookup", { profile_ids: Array(101).fill(ALEX) })).status, 400);
  assert.equal((await r.call("GET", `/v1/cosmetics/${ALEX}`)).status, 405);
  assert.equal((await r.call("GET", "/elsewhere")).status, 404);
});

test("doesn't serve records whose key has expired, and cleans them up", async () => {
  const r = relay();
  const expiring = playerKey(ALEX, Date.now() + 2000);
  assert.equal((await r.put(ALEX, record(ALEX, expiring, COSMETIC))).status, 204);
  assert.equal((await r.lookup([ALEX])).length, 1);
  r.env.DB.db.prepare("UPDATE cosmetics SET expires_at = ?").run(Date.now() - 1);
  assert.deepEqual(await r.lookup([ALEX]), []);
  await worker.scheduled({}, r.env);
  assert.equal(r.env.DB.db.prepare("SELECT COUNT(*) AS n FROM cosmetics").get().n, 0);
});

test("accepts a record signed by the mod", { skip: !process.env.ET_SYNC_SAMPLE && "ET_SYNC_SAMPLE is not set" }, async () => {
  const sample = JSON.parse(readFileSync(process.env.ET_SYNC_SAMPLE, "utf8"));
  const key = await crypto.subtle.importKey(
    "spki", Buffer.from(sample.mojang_public_key, "base64"), { name: "RSASSA-PKCS1-v1_5", hash: "SHA-1" }, false, ["verify"]);
  const result = await verifyRecord(sample.record, sample.record.profile_id, [key], Date.now());
  assert.equal(result.issuedAt, sample.record.issued_at);
  assert.equal(result.expiresAt, Date.parse(sample.record.key.expires_at.replace(/(\.\d{3})\d*Z$/, "$1Z")));
});
