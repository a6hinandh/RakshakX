# RakshakX Roadmap

This document tracks completed work and planned future development.

---

## Completed — v1.1.0

All four phases of the original roadmap have been implemented.

### Phase 1: Core Engine Improvements ✅

| Feature | Status | Notes |
|---------|--------|-------|
| Remove WhisperLite stubs | ✅ Done | Deleted `WhisperLiteStub.kt`, `WhisperLiteTranscriber.kt`; all transcription routes through Vosk |
| Cross-channel correlation expansion | ✅ Done | 5 strategies: SMS-Web, Email-Web, Call-SMS, Phone multi-channel, Temporal burst |
| Risk engine contextual scoring | ✅ Done | Sender reputation, time weighting, banking false-positive reduction, combination amplification |

### Phase 2: Feature Enhancements ✅

| Feature | Status | Notes |
|---------|--------|-------|
| Call overlay improvements | ✅ Done | Live transcript, confidence meter, End/Report/Safe quick actions, contextual fraud phrase warnings |
| Threat intelligence sharing | ✅ Done | `ThreatIntelligenceManager` with SHA-256 hashing, opt-in only, `ThreatIntelScreen` |
| Smart notification grouping | ✅ Done | ALERTS_CRITICAL / ALERTS_LOW / DIGEST channels, InboxStyle group summaries |
| Daily security digest | ✅ Done | `SecurityDigestWorker` via WorkManager, 24h threat summary |

### Phase 3: New Features ✅

| Feature | Status | Notes |
|---------|--------|-------|
| WhatsApp/messaging integration | ✅ Done | `RakshakNotificationListenerService` extended for WhatsApp, Telegram, Signal, etc. |
| UPI payment fraud detection | ✅ Done | `upi://pay` deep-link parsing, collect request detection, UPI keyword category in RiskEngine |
| Scam call database | ✅ Done | `ScamCallDatabase` with risk levels, auto-silence, community-reported numbers |
| Offline model updates (OTA) | ✅ Done | `ModelUpdateManager` with version tracking and rollback |
| Family protection mode | ✅ Done | `FamilyProtectionManager` with roles (Admin/Elder/Child/Self), simplified UI, `FamilyProtectionScreen` |

### Phase 4: Platform & Scale ✅

| Feature | Status | Notes |
|---------|--------|-------|
| Home screen widget | ✅ Done | `SecurityWidgetProvider` — 3x2 cells, 30min update, security score + active channels |
| Indic language localization | ✅ Done | Hindi, Tamil, Telugu, Kannada string resources |
| Lazy IndicBERT loading | ✅ Done | `ScamClassifierRouter` defers IndicBERT init until first Indic text detected |
| CI/CD pipeline | ✅ Done | GitHub Actions — build, test, lint, APK size tracking, artifact upload |
| Wear OS companion | ⬜ Not started | Requires separate Gradle module; lowest priority (P4) |

---

## Technical Debt — Remaining

| Item | Priority | Notes |
|------|----------|-------|
| Enable ProGuard/R8 for release | P1 | Security risk — release builds currently unobfuscated |
| Expand test coverage | P1 | Only 2 unit test files; need coverage for RiskEngine, ScamClassifierRouter, correlation logic |
| No crash reporting | P2 | No Firebase Crashlytics or equivalent |
| Hardcoded notification icons | P3 | Uses `android.R.drawable.ic_dialog_info` instead of branded icons |
| Vosk model OTA download | P3 | Model is bundled (~50MB), bloating APK; should be on-demand download |

---

## Future Roadmap

### Near-term (next release)

**Enable ProGuard** — The single most impactful security improvement for a security app. Release builds need minification and obfuscation.

**Test coverage expansion** — Target areas:
- `RiskEngine` scoring logic and edge cases
- `ScamClassifierRouter` language detection and fallback routing
- `MultiChannelCorrelationEngine` all five strategies
- `ThreatIntelligenceManager` hashing and blocklist lookup

**Branded notification icons** — Replace system drawables with RakshakX icons across all notification paths.

### Medium-term

**Wear OS companion** — Vibration alert on watch for CRITICAL threats, quick dismiss/report from wrist, security score complication. Requires adding a `wearapp` Gradle module.

**Firebase Test Lab integration** — Extend CI/CD to run instrumented tests on real devices via Firebase Test Lab, closing the gap between emulator-only CI and physical device behavior.

**INT4 model quantization** — Further reduce ONNX model size and inference latency beyond current INT8.

**Vosk on-demand download** — Move the bundled Vosk model (~50MB) to an on-demand download, reducing APK install size significantly.

### Long-term

**Additional Indic localization** — Extend string resources to all 11 supported detection languages (currently 4: Hindi, Tamil, Telugu, Kannada).

**Federated learning readiness** — Architecture to incorporate user corrections (false positive / false negative feedback) into model improvements without uploading raw data.

**NPCI UPI fraud integration** — Direct reporting path to the National Payments Corporation of India for confirmed UPI fraud cases.

**Cybercrime portal deep link** — Pre-filled report generator for filing complaints at cybercrime.gov.in with threat data exported from the app.

---

## Success Metrics

| Metric | Target |
|--------|--------|
| Detection precision | ≥ 95% on Indian scam datasets |
| Detection recall | ≥ 90% on Indian scam datasets |
| False positive rate | < 2% on legitimate banking messages |
| SMS/email classification latency | < 200ms on-device |
| Daily battery impact | < 3% with all channels active |
| Multi-channel correlation hit rate | ≥ 30% of CRITICAL threats correlated |
