# Albagram Dictionary API

Local Ktor service that aggregates Albanian dictionary lookups from:

- [fjalori.online](https://fjalori.online/) — Fjalor i Madh i Gjuhës Shqipe
- [fjalori.shkenca.org](http://www.fjalori.shkenca.org/) — Fjalor shpjegues (~40k fjalë)

## Run locally

```bash
./gradlew :dictionary-api:run
```

Server listens on `http://0.0.0.0:8080`.

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/health` | Liveness check |
| GET | `/v1/dictionary/search?q=&mode=exact\|prefix\|contains&limit=20` | Multi-source search |
| GET | `/v1/dictionary/lookup?term=&source=` | Single entry lookup |

## Android app configuration

Add to `local.properties` (optional):

```properties
DICTIONARY_API_BASE_URL=http://10.0.2.2:8080/
```

- **Emulator:** default `http://10.0.2.2:8080/` reaches the host machine.
- **Physical device:** use your PC LAN IP, e.g. `http://192.168.1.10:8080/`.

Start the API before using online glossary search in the app.

## Notes

- Results are cached in memory for 24 hours per source/query.
- Sources are queried with rate limiting (~1 req/s per source).
- If one source fails, partial results from other sources are still returned.
