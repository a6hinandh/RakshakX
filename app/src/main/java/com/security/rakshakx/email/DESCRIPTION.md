# Email Module

**17 files.** Notification-based email interception, multi-signal threat analysis, and persistence.

## Responsibilities

- Intercept email notifications from Gmail, Outlook, Yahoo Mail, ProtonMail, and other email clients
- Run multi-signal analysis: URL reputation, intent/urgency, obfuscation patterns, attachment risk
- Route high-confidence text to `ScamClassifierRouter` for hybrid ML + rules scoring
- Persist threats to the email-specific Room DB (`ThreatDatabase`)
- Contribute to cross-channel correlation via `MultiChannelCorrelationEngine`

## Sub-packages

| Package | Purpose |
|---------|---------|
| `analyzer/` | Individual signal analyzers: URL, intent, obfuscation, attachment |
| `database/` | `ThreatDatabase` (Room), `ThreatEntity`, `ThreatDao` |
| `pipeline/` | `EmailThreatPipeline` — orchestrates analyzers, persists if score ≥ 0.50 |
| `scoring/` | `ThreatCorrelationEngine` — email-level threat aggregation |

## Key Classes

| Class | Purpose |
|-------|---------|
| `EmailScamDetector` | Entry point; calls `RakshakXApplication.scamRouter.classify()` and passes result to pipeline |
| `EmailThreatPipeline` | Writes `ThreatEntity` to DB for high-risk emails; tracks duplicates |

## Detection Signals

- **URL analysis** — Suspicious domains, redirect obfuscation, known phishing TLDs
- **Intent analysis** — Urgency keywords, credential harvesting phrases, financial pressure
- **Obfuscation** — Homoglyphs, zero-width characters, base64-encoded content, excessive caps
- **Attachment risk** — APK, executable, and archive attachment indicators in notification text

## Scoring

```
EmailScamDetector invokes ScamClassifierRouter.classify(text, "email")
  → finalScore = ML × 0.60 + Rules × 0.40
  → If finalScore ≥ 0.50: EmailThreatPipeline persists to ThreatDatabase
  → If finalScore ≥ 0.70: ALERTS_CRITICAL notification channel
```

## Supported Email Apps

Identified by package name in `RakshakNotificationListenerService`:
- `com.google.android.gm` (Gmail)
- `com.microsoft.office.outlook`
- `com.yahoo.mobile.client.android.mail`
- `ch.protonmail.android`
- `me.proton.android.mail`
- Other mail clients via configurable package list

## Notes

- Gmail API integration is not used — the module is notification-only to preserve privacy
- The email-specific `ThreatDatabase` is separate from the unified `FraudDao` database; both are SQLCipher-encrypted
