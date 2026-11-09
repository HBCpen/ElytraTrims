# Cosmetic relay

A small [Cloudflare Worker](https://developers.cloudflare.com/workers/) with a [D1](https://developers.cloudflare.com/d1/)
database that lets players of Elytra Trims Cosmetic see each other's cosmetics. Vanilla servers don't forward data
between clients, so the mod exchanges cosmetics through this relay instead.

The relay only stores the latest cosmetic of each player and hands them out by UUID.

## Security

Each cosmetic is signed with the player's chat signing key. Mojang issues that key and certifies it for the player's
UUID; vanilla uses the same key to verify chat messages.

- **Publishing:** the relay checks Mojang's certificate and the signature before storing a cosmetic, so nobody can
  overwrite someone else's.
- **Fetching:** clients verify every cosmetic again themselves, so a broken or malicious relay can't make anyone show a
  cosmetic they didn't publish.
- **Mojang's keys:** Mojang's API refuses requests from Cloudflare Workers, so the relay ships Mojang's current
  certificate keys in `src/mojang-keys.js` and uses any it manages to fetch in addition. If Mojang ever rotates them,
  update that file and deploy again.
- **Requirements:** only players signed in with a Microsoft account (online mode) have such a key, so only they can
  share a cosmetic. Players on offline-mode servers aren't looked up, since their UUIDs aren't the ones of their account.

Shared cosmetics are public: anyone can fetch them by UUID. Clients look up the UUIDs of all players they track
nearby, including players without this mod, so the operator of the relay can see each client's IP address together
with who was near that player and when. The relay doesn't log or store lookups, but Cloudflare may keep request logs.

## Free plan

The Workers Free plan is enough for small and medium groups.

| Limit (per day, Free plan) | Used by the mod |
| --- | --- |
| 100,000 Worker requests | About 1 request every 10 seconds while new players come into view, then 1 per 5 minutes per group of up to 100 players nearby; 1 request when your cosmetic or key changes |
| 5 million D1 rows read | 1 row per player looked up |
| 100,000 D1 rows written | 1 row per published change |

When a daily limit is reached, requests fail until 00:00 UTC; nothing is charged on the Free plan.

## Rate limits

`wrangler.toml` limits each client IP (per Cloudflare location) to 60 lookups and 10 publications per minute, using
[Workers rate limiting](https://developers.cloudflare.com/workers/runtime-apis/bindings/rate-limit/). A client looks up
at most once per 10 seconds, so this leaves room for several players behind one address. Limited requests are answered
with `429 Too Many Requests` before the database is touched, which keeps one client from using up the D1 quota and CPU
time. They still count as Worker requests, though: blocking floods before they reach the Worker needs a custom domain
with Cloudflare's WAF rate limiting rules.

On the Free plan with a `workers.dev` address, the bindings were found not to limit anything: hundreds of requests
per minute from one IP all passed. Counting in the Worker's own memory doesn't help either, since Cloudflare spreads
one client's requests over many Worker instances. On the Free plan this only risks the relay running out of its daily
requests, not any charges. See the current
[Workers](https://developers.cloudflare.com/workers/platform/pricing/) and
[D1](https://developers.cloudflare.com/d1/platform/pricing/) pricing.

## Deploying

Needs [Node.js](https://nodejs.org/) and a Cloudflare account. Run these in this directory:

```sh
npx wrangler login
npx wrangler d1 create elytratrims-cosmetics
```

Copy the printed `database_id` into `wrangler.toml` (it holds the id of the fork's own relay), then:

```sh
npx wrangler d1 execute elytratrims-cosmetics --remote --file=schema.sql
npx wrangler deploy
```

`deploy` prints the URL of the relay, such as `https://elytratrims-cosmetic-relay.<account>.workers.dev`.
Each player sets it as `relay_url` in `config/elytratrims_cosmetic.json`:

```json
{
  "sync": {
    "mode": "share",
    "relay_url": "https://elytratrims-cosmetic-relay.<account>.workers.dev"
  }
}
```

## API

| Request | Body | Response |
| --- | --- | --- |
| `PUT /v1/cosmetics/{uuid}` | A signed record, see `verifyRecord` in `src/index.js` | `204`, or an error |
| `POST /v1/cosmetics/lookup` | `{"profile_ids": [uuid, ...]}`, up to 100 | `{"records": [record, ...]}` |

## Tests

```sh
npm test
```

The tests run the relay against an in-memory SQLite database (Node.js 22.5 or newer) and need no other packages.
To also check a record signed by the mod, run the client game tests and pass the sample they write:

```sh
ET_SYNC_SAMPLE=../versions/26.3-fabric/build/run/clientGameTest/elytratrims_sync_sample.json npm test
```
