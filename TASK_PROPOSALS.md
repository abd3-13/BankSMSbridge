# Codebase Task Proposals

## 1) Typo fix task
**Title:** Fix misspelled CBE debit regex token (`transfered` → `transferred`)

**Issue found:** The CBE debit parser regex currently looks for `transfered` (single `r`). If incoming SMS uses the correct spelling (`transferred`), the amount is never parsed.

**Scope:** `app/src/main/java/com/abdelah/banksms/parser/SmsParser.kt`

**Proposed change:**
- Update the CBE debit regex to support the correctly spelled word.
- Consider backward compatibility by matching both spellings in one pattern.

**Acceptance criteria:**
- CBE debit messages containing `transferred ETB ...` are parsed successfully.
- Legacy messages with `transfered ETB ...` (if any) still parse.

---

## 2) Bug fix task
**Title:** Parse BoA messages by sender ID when message body omits full bank name

**Issue found:** Bank detection for BoA depends only on message content (`"Bank of Abyssinia"`). Messages from BoA shortcodes/sender IDs without that exact phrase will not be parsed.

**Scope:**
- `app/src/main/java/com/abdelah/banksms/parser/SmsParser.kt`
- `app/src/main/java/com/abdelah/banksms/SmsReceiver.kt` (validation path)

**Proposed change:**
- Extend bank routing logic to include sender-based matching for BoA, similar to existing CBE sender fallback.
- Add deterministic checks for common BoA sender variants.

**Acceptance criteria:**
- A BoA debit/credit SMS from a BoA sender parses even if the body lacks `Bank of Abyssinia`.
- Existing content-based BoA parsing remains unchanged.

---

## 3) Code comment/documentation discrepancy task
**Title:** Align comment and implementation for transaction ordering semantics

**Issue found:** `TransactionDao.getByStatus()` returns rows ordered by `dateTime ASC` (oldest first), but the typical expectation for pending work queues is newest first; the current API has no comment clarifying this behavior.

**Scope:** `app/src/main/java/com/abdelah/banksms/db/TransactionDao.kt`

**Proposed change:**
- Either:
  1) keep ascending order and add explicit KDoc/comment documenting “oldest-first processing”, or
  2) switch to `DESC` and document “newest-first view”.
- Ensure callers are consistent with the chosen direction.

**Acceptance criteria:**
- Query ordering is intentionally documented adjacent to the DAO method.
- Team can tell at a glance whether processing is FIFO-style or latest-first.

---

## 4) Test improvement task
**Title:** Add parser unit tests for real-world SMS variants and fallback behavior

**Issue found:** No tests currently exist for parser behavior, leaving regressions (regex spelling variants, sender-vs-body routing, reference extraction) undetected.

**Scope:**
- New tests under `app/src/test/...` targeting `SmsParser.parse()`

**Proposed change:**
- Add table-driven tests for each bank parser (CBE, BoA, telebirr, Enat).
- Include negative tests (unknown sender/content returns `null`).
- Add coverage for date fallback path when message date extraction fails.

**Acceptance criteria:**
- Unit test suite validates debit + credit happy paths per supported provider.
- Test suite covers at least one typo/variant case and one sender-based detection case.
- Tests run via Gradle and fail on parser regressions.
