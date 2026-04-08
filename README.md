# Bank SMS Bridge

Bank SMS Bridge is an Android app designed to act as a **bridge between banking SMS alerts and personal finance tracking systems**.

## Purpose

The full purpose of this app is to:

1. Receive transaction SMS alerts from banks and wallets on Android.
2. Parse those messages into structured transaction records.
3. Store the parsed transactions locally on the device.
4. When a Firefly III instance is reachable, post those transactions to Firefly III.
5. Retry failed syncs until they are successfully delivered.

In short, this project aims to automate the flow from **SMS notification → clean transaction data → Firefly III ledger**.

---

## Current Capability (Implemented)

At the moment, the app already supports the first half of that flow:

- **Receives incoming SMS** using an Android `BroadcastReceiver`.
- **Parses supported SMS formats** into a normalized transaction model.
- **Persists parsed transactions locally** using Room (`transactions` table).
- Tracks transaction state with a `status` field (`PENDING`, `SENT`, `FAILED`) to support future delivery workflows.

### Supported senders/formats

The parser currently includes bank/wallet-specific logic for:

- CBE (Commercial Bank of Ethiopia)
- BOA (Bank of Abyssinia)
- Telebirr / Ethio telecom messages
- Enat Bank
- etc..

It extracts key transaction fields such as:

- bank/source
- debit/credit type
- amount
- currency (default ETB)
- date/time
- description
- reference identifier
- raw message text (for traceability)

---

## Sync Capability (Implemented)

The Firefly III synchronization loop is now implemented:

- A WorkManager-based background worker reads `PENDING` and `FAILED` rows.
- The worker checks network availability before attempting sync.
- Transactions are transformed into Firefly III transaction payloads and posted via API token auth.
- Successful posts are marked `SENT`.
- Failures are marked `FAILED` and retried with exponential backoff.
- A periodic sync job is scheduled when the app starts, and immediate one-off sync is triggered when a new transaction is saved.

This ensures transactions are retried and eventually delivered when connectivity or API availability recovers.

---

## High-Level Architecture

- **SMS Ingestion**: `SmsReceiver` listens to `android.provider.Telephony.SMS_RECEIVED`.
- **Parsing Layer**: `SmsParser` routes messages to bank-specific regex parsers.
- **Local Storage**: Room database (`AppDatabase`, `TransactionDao`, `TransactionEntity`).
- **UI**: Minimal Compose activity confirming the app is running.
- **Sync Engine**: WorkManager + sync repository/client for Firefly III posting and retries.

---

## Android Permissions Used

- `RECEIVE_SMS`
- `READ_SMS`
- `INTERNET`
- `ACCESS_NETWORK_STATE`

These are required for receiving/parsing SMS and later syncing to Firefly III over network.

---

## Build & Run

### Requirements

- Android Studio (latest stable recommended)
- Android SDK with minSdk 24+
- JDK 11

### Build from command line

```bash
./gradlew :app:assembleRelease 
```

### Run

1. Install the debug APK on an Android device (or compatible emulator with SMS support).
2. Open the app and grant SMS permissions.
3. Send/sample supported bank SMS messages.
4. Verify records are created in local Room storage.

---
