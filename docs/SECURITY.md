# RakshakX Security Model

This document describes the security architecture, privacy guarantees, and threat model for RakshakX.

---

## Core Principles

1. **On-device only** — All ML inference, text analysis, and scoring runs locally. No user data leaves the device.
2. **Encrypted at rest** — All threat data is stored in SQLCipher (AES-256-CBC) encrypted Room databases.
3. **Minimal data collection** — The app processes messages to detect threats but does not retain raw message content beyond what's needed for the threat log.
4. **User control** — Every detection channel can be independently enabled/disabled. Threat intelligence sharing is strictly opt-in.

---

## Data Protection

### Storage Encryption

All Room databases are encrypted with SQLCipher 4.6.1:

```
DatabaseFactory.getInstance(context)
  → SQLCipher passphrase derived from Android Keystore
  → AES-256-CBC encryption for all database files
  → WAL mode for concurrent read/write performance
```

The encryption key is managed by `androidx.security.crypto` and backed by the Android Keystore system, making it hardware-protected on supported devices.

### Data at Rest

| Data Type | Storage | Encryption | Retention |
|-----------|---------|------------|-----------|
| SMS threat records | Room (SQLCipher) | AES-256 | Until user clears |
| Call threat records | Room (SQLCipher) | AES-256 | Until user clears |
| Email threat records | Room (SQLCipher) | AES-256 | Until user clears |
| Correlated sessions | Room (SQLCipher) | AES-256 | Until user clears |
| App settings | EncryptedSharedPreferences | AES-256 | Persistent |
| ML models | App assets | N/A (non-sensitive) | Bundled with APK |

### Data in Transit

RakshakX does not transmit user data to any server. The only network operations are:

1. **VPN DNS relay** — DNS queries are forwarded to the device's configured DNS servers (not to RakshakX servers)
2. **Model update checks** — Checks a configured endpoint for newer model versions (metadata only, no user data sent)
3. **Threat intelligence sharing** (opt-in only) — Only SHA-256 hashed phone numbers and domains are shared, never raw content

---

## Threat Intelligence Privacy

When users opt into threat intelligence sharing via `ThreatIntelligenceManager`:

### What IS shared (hashed)
- SHA-256 hash of reported phone numbers
- SHA-256 hash of reported domains
- Threat category (e.g., "PHISHING", "VISHING")
- Timestamp (rounded to nearest hour for k-anonymity)

### What is NEVER shared
- Raw phone numbers or domains
- Message content (SMS, email, call transcripts)
- User identity or device information
- Contact lists or call history
- Location data

### Hashing implementation
```kotlin
MessageDigest.getInstance("SHA-256")
    .digest(rawValue.toByteArray())
    .joinToString("") { "%02x".format(it) }
```

One-way SHA-256 hashes cannot be reversed to recover the original phone number or domain.

---

## Permission Model

RakshakX requests sensitive permissions. Each is justified by a specific detection capability:

| Permission | Justification | Channel |
|-----------|---------------|---------|
| `RECEIVE_SMS` / `READ_SMS` | SMS threat ingress and inbox polling | SMS |
| `READ_CALL_LOG` / `READ_PHONE_STATE` | Call state monitoring, caller ID | Call |
| `RECORD_AUDIO` | Call transcription for fraud analysis | Call |
| `SYSTEM_ALERT_WINDOW` | Real-time fraud overlay during calls | Call |
| `POST_NOTIFICATIONS` | Alert delivery | All |
| `INTERNET` | VPN DNS relay, model updates | Web |
| `CAMERA` | QR code scanning for malicious URLs | Web |
| NotificationListenerService | SMS/email/messaging notification interception | SMS, Email, Messaging |
| AccessibilityService | Browser URL monitoring | Web |
| VpnService | Network traffic analysis | Web |

### Progressive Onboarding

Permissions are not requested all at once. `OnboardingActivity` walks users through each permission with clear explanations. The app enforces minimum permissions before dashboard access via `PermissionManager.getReadinessState()`.

### Runtime Guards

Every notification-posting path checks `PermissionManager.hasNotificationPermission()` at runtime. This prevents crashes on Android 13+ where `POST_NOTIFICATIONS` is a runtime permission.

---

## VPN Security

### Architecture

`FraudVpnService` creates a local TUN interface that intercepts DNS queries only:

```
App network request → Android routes DNS to TUN (10.0.0.1)
  → DnsVpnRelay reads DNS packet
  → PacketParser extracts query domain
  → Domain analyzed against threat intelligence
  → DNS forwarded to real DNS server
  → Response returned through TUN
```

### What the VPN CAN detect
- Domain names being resolved (DNS queries)
- Redirect chains (multiple DNS lookups in sequence)
- Known malicious domains

### What the VPN CANNOT detect
- Encrypted HTTPS payload content
- POST body data (credentials, form submissions)
- Anything beyond DNS metadata

### No MITM
The VPN does not install a custom CA certificate and does not attempt to decrypt TLS traffic. This is a deliberate design choice — MITM inspection would compromise the security of all HTTPS connections.

---

## Scam Call Database Security

`ScamCallDatabase` maintains a local database of reported scam numbers:

- Numbers are stored as-is locally (needed for caller ID matching)
- Community-shared numbers use SHA-256 hashes
- The database is stored within the SQLCipher-encrypted Room database
- Auto-silence of known scam numbers is opt-in and can be disabled

---

## Family Protection Security

`FamilyProtectionManager` enables remote monitoring for family members:

- Requires explicit consent from the monitored family member
- Uses role-based access (Admin, Elder, Child, Self)
- Alert sharing is limited to threat notifications — raw message content is never shared
- Simplified UI mode for elderly users reduces attack surface (fewer controls to accidentally change)

---

## ML Model Security

### Model Integrity

- ONNX models are bundled in the APK and verified at build time via `verifyRakshakOnnxAssets` Gradle task
- Minimum file size checks prevent shipping stub/empty models
- OTA model updates verify version numbers and support rollback if a new model underperforms

### Model Limitations

ML models can be evaded by adversarial text (deliberately crafted messages that avoid detection patterns). The hybrid ML + rules approach mitigates this:
- Rule-based scoring catches keyword patterns even if ML is fooled
- Combination amplification (e.g., urgency + credential keywords) provides a safety net
- Sender reputation tracking escalates scores for repeat offenders regardless of message content

---

## Known Security Considerations

1. **ProGuard disabled** — Release builds do not currently enable code minification/obfuscation. This means the APK can be reverse-engineered to understand detection patterns. Enabling ProGuard is tracked as technical debt.

2. **Accessibility service scope** — The accessibility service for web monitoring has broad permissions. It is limited to URL bar monitoring in browsers but users should understand the permission scope.

3. **Audio recording** — Call recording for fraud analysis is a sensitive capability. The app records only during active calls and only when call monitoring is enabled. Recordings are processed locally and not stored after transcription.

4. **Notification content access** — `NotificationListenerService` can read all notifications. RakshakX filters for specific package names (SMS, email, messaging apps) and ignores others.

---

## Vulnerability Reporting

If you discover a security vulnerability in RakshakX, please report it responsibly:

1. **Do not** open a public GitHub issue for security vulnerabilities
2. Email the maintainers directly with a description of the vulnerability
3. Include steps to reproduce and potential impact assessment
4. Allow reasonable time for a fix before public disclosure

---

## Compliance Notes

- RakshakX processes data under legitimate interest (user-initiated device security)
- No data is transmitted to third parties (except opt-in hashed threat intelligence)
- Users can delete all stored threat data via app settings
- The app does not collect analytics, telemetry, or usage metrics
