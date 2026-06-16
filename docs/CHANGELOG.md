# Changelog

All notable changes to RakshakX are documented in this file.

---

## [1.1.0] — 2026-06-15

### Added

**Phase 1: Core Engine Improvements**
- Removed WhisperLite dead code stubs (`WhisperLiteStub.kt`, `WhisperLiteTranscriber.kt`)
- Added file-based transcription API to `VoskTranscriber` for backward compatibility
- Expanded `MultiChannelCorrelationEngine` with 5 correlation strategies:
  - SMS-Web URL correlation (1h window)
  - Email-Web URL correlation (1h window)
  - Call-SMS phone correlation (15min window)
  - Phone multi-channel correlation (24h window)
  - Temporal burst detection (15min window)
- Enhanced `RiskEngine` with contextual weighted scoring:
  - Sender reputation tracking
  - Time-of-day weighting (late night 1.2x, evening 1.15x, business hours 0.95x)
  - Banking false-positive reduction
  - Combination amplification (urgency + credential = +10)
  - New keyword categories: UPI/payment fraud, job/investment scams

**Phase 2: Feature Enhancements**
- Real-time call overlay redesign with premium dark UI
  - Live transcript display, confidence meter
  - Quick action buttons: End Call, Report Number, Mark Safe
  - Contextual fraud phrase warnings (OTP, bank, arrest, remote access)
- Threat intelligence sharing (opt-in, SHA-256 differential privacy)
  - `ThreatIntelligenceManager` singleton with community blocklist
  - `ThreatIntelScreen` with privacy guarantees UI
- Smart notification grouping by severity
  - Dedicated channels: ALERTS_CRITICAL, ALERTS_LOW, DIGEST
  - InboxStyle group summary notifications
- Daily security digest via `SecurityDigestWorker` (WorkManager)

**Phase 3: New Features**
- WhatsApp/Telegram/Signal message interception via `RakshakNotificationListenerService`
  - Forwarded message detection
  - App-specific analysis (WhatsApp, Telegram, Signal, Google Messages, etc.)
- UPI payment fraud detection
  - `upi://pay` deep-link pattern matching
  - UPI collect request scam detection
  - Clipboard monitoring for suspicious UPI IDs
- Scam call database (`ScamCallDatabase`)
  - Pre-call screening with risk levels
  - Community-reported numbers
  - Auto-silence for known scam numbers
- Offline model updates (`ModelUpdateManager`)
  - OTA model download without app update
  - Version tracking and rollback support
  - WorkManager periodic checks (12h interval)
- Family protection mode (`FamilyProtectionManager`)
  - Role-based access: Admin, Elder, Child, Self
  - Simplified UI mode for elderly users
  - Family member management
  - `FamilyProtectionScreen` UI

**Phase 4: Platform & Scale**
- Home screen widget (`SecurityWidgetProvider`)
  - Security score display, active channel count
  - 3x2 cells, 30-minute update interval
- Localization for 4 Indian languages (Hindi, Tamil, Telugu, Kannada)
- Lazy IndicBERT loading (saves ~200MB for English-only users)
- GitHub Actions CI/CD pipeline
  - Automated debug builds, unit tests, lint checks
  - APK size tracking in job summary
  - Artifact uploads (APK, test results, lint report)

### Changed
- `ScamClassifierRouter` now lazy-loads IndicBERT on first Indic text
- `RakshakXActivity` switched from WhisperLite to Vosk transcriber
- `UrlScanActivity` handles nullable `sourceSms` with email-web fallback
- `SmsReceiver` now records flagged senders and triggers cross-channel correlation
- Notification channels expanded from 2 to 5

### Fixed
- Nullable crash in `UrlScanActivity` when correlation source is email (not SMS)
- Missing coroutine imports in `OverlayBubbleService` for Report Number action
- Widget background drawable now available in both base `drawable/` and `drawable-v26/`

---

## [1.0.0] — Initial Release

### Features
- Multi-channel threat detection: SMS, Email, Call, Web
- Hybrid ML classification: DistilBERT (English) + IndicBERT (11 Indic languages)
- On-device ONNX Runtime inference
- Vosk speech-to-text for call transcription
- VPN-based web protection with DNS analysis
- Cross-channel correlation engine
- Room + SQLCipher encrypted database
- Jetpack Compose UI with Material3
- Progressive permission onboarding
- QR code scanning for malicious URLs
