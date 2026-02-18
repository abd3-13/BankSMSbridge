# AGENTS.md

Project: SMS to Firefly III Android Sync App

Language: Kotlin

Architecture: Android-only (no backend)

Mission:
This app listens to bank SMS messages, parses transactions,
deduplicates them, and posts them to Firefly III.
Financial correctness and idempotency are critical.

Core Principles:
- Never create duplicate transactions.
- Never post partially parsed or uncertain transactions.
- Fail safely rather than guess.
- Financial data integrity is more important than UX speed.
- Code must be deterministic and testable.

Architecture Rules:
- Use clear separation:
    SMS Listener → Parser → Normalizer → Deduplicator → API Client
- Parsing logic must not directly call network APIs.
- Deduplication must occur before API submission.
- API client must be isolated from Android UI components.

Parsing Rules:
- Parsing must tolerate minor wording variations.
- Always normalize:
    - amount
    - currency
    - bank name
    - timestamp
- Extract a stable fingerprint:
    hash(amount + timestamp + reference + bank name)

Deduplication:
- Store fingerprint locally (Room/SQLite).
- Enforce uniqueness at database level.
- Never rely only on in-memory checks.

Networking Rules:
- Use suspend functions (coroutines).
- Implement retry with exponential backoff.
- Handle 4xx and 5xx responses differently.
- Never log API tokens.
- Never log full SMS in production logs.

Security:
- Store Firefly API token securely (EncryptedSharedPreferences).
- Never expose secrets in logs or crash reports.

Android Constraints:
- Must handle:
    - app killed state
    - device reboot
    - duplicate SMS delivery
- Avoid blocking main thread.
- Use WorkManager for background retry tasks

Performance:
- Keep memory footprint low.
- Avoid heavy dependencies.
- Avoid reflection-heavy libraries.

Future-proofing:
- Parser logic must be modular to support multiple banks.
