# `com.security.rakshakx` — Package Reference

Detailed codemap for engineers onboarding onto RakshakX or contributors who need a clear mental model of the codebase. For system-level architecture, see [ARCHITECTURE.md](ARCHITECTURE.md).

---

## Package Overview

```
com.security.rakshakx/
├── call/           # Call channel — 54 files
├── web/            # Web channel — 34 files
├── email/          # Email channel — 17 files
├── sms/            # SMS channel — 7 files
├── integration/    # Shared ML — 6 files
├── notifications/  # Notification plumbing — 7 files
├── core/           # Cross-cutting utilities — 8+ files
│   ├── correlation/
│   ├── threatintel/
│   ├── family/
│   ├── callerid/
│   └── modelupdate/
├── data/           # Unified data layer — 3 files
├── ui/             # Compose UI — 18 files
├── permissions/    # Permission model — 1 file
├── onboarding/     # Permission setup flow — 1 file
├── startup/        # Boot orchestration — 1 file
└── widget/         # Home screen widget — 1 file
```

---

## `call/` — Call Channel

**The largest package (54 files).** Handles the full call fraud detection pipeline: state monitoring, audio capture, transcription, overlay UI, and ML inference.

### `call/callanalysis/`

| Class | Purpose |
|-------|---------|
| `VoskTranscriber` | Streaming and file-based speech-to-text using Vosk 0.3.38. Handles WAV header skipping, partial/final result accumulation, and `isModelAvailable()` check |
| `CallStateMonitor` | Listens to telephony state changes; triggers recording and overlay on OFFHOOK |
| `OverlayBubbleService` | `SYSTEM_ALERT_WINDOW` floating overlay shown during calls. Displays live transcript, risk meter, contextual fraud phrase warnings, and quick action buttons (End Call, Report, Mark Safe) |
| `OverlayBubbleManager` | Manages overlay lifecycle and binding |

### `call/callanalysis/ui/`

| Class | Purpose |
|-------|---------|
| `RakshakXActivity` | Debug/test activity for call analysis; wires `VoskTranscriber` for manual testing |

### `call/core/`

| Class | Purpose |
|-------|---------|
| `CallOrchestrator` | Coordinates recording, transcription, and inference for a single call |
| `FraudInferenceEngine` | Call-specific ML model for fraud intent classification |
| `call/core/storage/` | Room DB for call records, `DatabaseFactory` for encrypted database access |

### `call/services/`

| Class | Purpose |
|-------|---------|
| `CallAnalysisService` | Foreground service coordinating call monitoring lifecycle |
| `CallRecordingService` | Audio capture from the microphone during calls |
| `RiskScanWorker` | WorkManager worker for background call risk scanning |
| `HackathonModeCallMonitorService` | Demo/testing service that simulates call fraud without real calls |

---

## `web/` — Web Channel

**34 files.** VPN-based traffic inspection, accessibility service for browser monitoring, URL scanning, and domain analysis.

### `web/services/`

| Class | Purpose |
|-------|---------|
| `FraudVpnService` | Local TUN VPN that intercepts DNS queries. Orchestrates `DnsVpnRelay`, `PacketParser`, domain analysis, and threat blocking |
| `DnsVpnRelay` | Pure-Kotlin DNS relay; forwards queries to real DNS servers while extracting domain names for analysis |

### `web/analyzers/`

| Class | Purpose |
|-------|---------|
| `DomainRiskAnalyzer` | Checks domain against threat intelligence, TLS mismatch, redirect count, DNS flags |
| `FraudRiskAnalyzer` | Full-spectrum fraud scoring combining domain risk, AI scorer, and session data |
| `BrowserNetworkCorrelationEngine` | Links active browser session (URL, form fields) to DNS-level domain events |
| `ThreatBlockingEngine` | Decides ALLOW / WARN / BLOCK for a domain based on threat assessment and action |
| `ScamLanguageAnalyzer` | Keyword and pattern matching on visible page text |
| `ThreatScoringEngine` | Aggregates signals from multiple analyzers into a final threat score |
| `ThreatIntelRepository` | Local threat intelligence store used by web analyzers |

### `web/extractors/`

| Class | Purpose |
|-------|---------|
| `PacketParser` | Parses raw TUN packets to extract source/dest IP, port, and protocol |
| `DnsTrafficAnalyzer` | Extracts domain name and DNS flags from parsed DNS packets |
| `RedirectChainTracker` | Tracks DNS lookup sequences to identify redirect chains |

### `web/ai/`

| Class | Purpose |
|-------|---------|
| `ModelManager` | Loads TFLite or ONNX models for web-specific fraud scoring; graceful fallback to heuristics |
| `AiThreatScorer` | Wraps a loaded model to score URL/page-text pairs |
| `FraudTextPreprocessor` | Tokenizes and normalizes text for web ML models |

### `web/ui/`

| Class | Purpose |
|-------|---------|
| `UrlScanActivity` | Scan a URL manually or via QR code; shows correlation with recent SMS/email |

---

## `email/` — Email Channel

**17 files.** Notification-based email extraction, multi-signal threat analysis, and persistence.

| Class | Purpose |
|-------|---------|
| `EmailScamDetector` | Orchestrates email classification via `ScamClassifierRouter` |
| `EmailThreatPipeline` | Persistence and tracking; writes to email Room DB if score ≥ 0.50 |
| `email/analyzer/` | URL analyzer, intent analyzer, obfuscation detector, attachment risk scorer |
| `email/database/` | Room DB (`ThreatDatabase`, `ThreatEntity`, `ThreatDao`) for email threat records |
| `email/scoring/ThreatCorrelationEngine` | Email-specific correlation logic |

---

## `sms/` — SMS Channel

**7 files.** Triple-ingress SMS detection with deduplication.

| Class | Purpose |
|-------|---------|
| `SmsScamDetector` | Main classification entry point for SMS text |
| `SmsReceiver` | `SMS_RECEIVED` broadcast receiver; records flagged senders; triggers cross-channel correlation after DB insert |
| `SmsPollingWorker` | WorkManager-based inbox polling fallback |
| `SmsDeduplicationGuard` | Time-window hash deduplication across all three ingress paths |
| `RiskEngine` | Contextual rule-based scorer (companion object); stateless |

---

## `integration/` — Shared ML

**6 files.** The only package all detection channels share for ML inference.

| Class | Purpose |
|-------|---------|
| `ScamClassifierRouter` | Hybrid decision logic: language detection → DistilBERT or IndicBERT → weighted hybrid score. DistilBERT is eager; IndicBERT is lazy-loaded on first Indic text |
| `DistilBertClassifier` | ONNX-based WordPiece tokenizer + DistilBERT inference wrapper |
| `IndicBertClassifier` | ONNX-based SentencePiece tokenizer + IndicBERT inference wrapper |
| `ModelResult` | Return type: `isScam`, `confidence`, `label`, `modelUsed`, `ruleScore`, `finalScore` |

---

## `notifications/` — Notification Plumbing

**7 files.** The single `NotificationListenerService` and all alert/digest infrastructure.

| Class | Purpose |
|-------|---------|
| `RakshakNotificationListenerService` | Single listener for all notification-based ingress: SMS apps, email apps, WhatsApp, Telegram, Signal, and others. Routes to per-channel detectors, detects UPI threats, handles forwarded message signals |
| `SmsFraudNotifications` | Builds grouped fraud notifications with InboxStyle summaries; routes to ALERTS_CRITICAL or ALERTS_LOW channel by severity |
| `RakshakNotificationChannels` | Defines 5 notification channels: `rakshak_fg`, `rakshak_alerts`, ALERTS_CRITICAL, ALERTS_LOW, DIGEST |
| `SecurityDigestWorker` | WorkManager `CoroutineWorker`; queries last 24h of threats; sends daily summary notification |

---

## `core/` — Cross-Cutting Utilities

### `core/correlation/`

| Class | Purpose |
|-------|---------|
| `MultiChannelCorrelationEngine` | 5 correlation strategies with time-windowed queries against `FraudDao`. Returns `CorrelationResult` with source entities and `CorrelationType` |
| `CorrelationEngine` | Original single-channel correlation engine (SMS-Web only); kept for backward compatibility |

### `core/threatintel/`

| Class | Purpose |
|-------|---------|
| `ThreatIntelligenceManager` | Singleton. Opt-in threat reporting with SHA-256 hashing. Maintains local blocklist from community data. Methods: `reportThreat()`, `isKnownThreat()`, `getStats()` |

### `core/family/`

| Class | Purpose |
|-------|---------|
| `FamilyProtectionManager` | Singleton. Role-based family protection (Admin/Elder/Child/Self). Manages simplified UI mode and family member list |

### `core/callerid/`

| Class | Purpose |
|-------|---------|
| `ScamCallDatabase` | Singleton. Risk levels per phone number. Auto-silence capability. Community-reported and user-reported number tracking |

### `core/modelupdate/`

| Class | Purpose |
|-------|---------|
| `ModelUpdateManager` | OTA model update framework. Version tracking, rollback support, WorkManager periodic check (12h interval) |

### `core/`

| Class | Purpose |
|-------|---------|
| `SettingsStore` | `StateFlow`-backed settings store; single source of truth for channel toggles and preferences |

---

## `data/` — Unified Data Layer

| Class | Purpose |
|-------|---------|
| `FraudDao` | Room DAO with queries for all entity types (SMS, Call, Email, Sessions). Key queries: `findRecentSmsByPhone`, `findCallsInTimeRange`, `findEmailsInTimeRange`, `getRecentSessionsList` |
| `FraudEntities` | `SmsEventEntity`, `CallEventEntity`, `EmailEventEntity`, `ThreatSessionEntity` |
| `FraudRepository` | Repository layer abstracting DAO access for UI/ViewModel consumers |

---

## `ui/` — Compose UI

**18 files.** All Jetpack Compose screens, shared components, and theme.

### Screens

| Screen | Route | Description |
|--------|-------|-------------|
| `HomeDashboardScreen` | `home` | Security score, channel status cards, cloud icon → ThreatIntel |
| `LiveThreatScreen` | `live_threat` | Real-time threat feed |
| `ThreatLogsScreen` | `threat_logs` | Historical threat log with filters |
| `CorrelationScreen` | `correlation` | Cross-channel correlation view, demo scenario toggle |
| `ScanningScreen` | `scanning` | QR code and manual URL scanning |
| `SettingsScreen` | `settings` | Channel toggles, thresholds, privacy settings |
| `PrivacyScreen` | `privacy` | Data management, database clear |
| `ThreatIntelScreen` | `threat_intel` | Opt-in toggle, privacy guarantees, community stats |
| `FamilyProtectionScreen` | `family_protection` | Role selection, member management, simplified UI toggle |

### Components & Theme

| File | Purpose |
|------|---------|
| `ui/components/Components.kt` | Shared Compose components (stat cards, section headers, threat chips) |
| `ui/theme/Color.kt` | Premium dark palette: Deep Space (#080C14), Royal Blue (#3B6DE6), etc. |
| `ui/theme/Theme.kt` | `RakshakXTheme` with Material3 dark color scheme and Compose theming |
| `ui/navigation/NavHost.kt` | Navigation graph; `Screen` sealed class; hidden routes for detail screens |

---

## `permissions/` — Permission Model

| Class | Purpose |
|-------|---------|
| `PermissionManager` | Canonical readiness model. `ReadinessState` enum. `getReadinessState(context)` checks notification listener, accessibility service, VPN readiness. `hasNotificationPermission()` runtime guard |

---

## `startup/` — Boot Orchestration

| Class | Purpose |
|-------|---------|
| `AppStartupCoordinator` | Startup contract authority. Called by `MainActivity` on launch and `BootReceiver` on device boot. Reconciles monitoring services against current permission state |

---

## `onboarding/` — Permission Setup

| Class | Purpose |
|-------|---------|
| `OnboardingActivity` | Progressive permission request flow. Gates dashboard entry until minimum permissions are satisfied |

---

## `widget/` — Home Screen Widget

| Class | Purpose |
|-------|---------|
| `SecurityWidgetProvider` | `AppWidgetProvider` showing security score and active channel count. 3x2 cells, 30-minute update, dark semi-transparent background |

---

## Data Flow: Notification → Alert

```
1. RakshakNotificationListenerService.onNotificationPosted()
   ↓
2. Classify source package:
   - isSmsAppPackage()      → SmsScamDetector
   - isEmailAppPackage()    → EmailScamDetector
   - isMessagingAppPackage() → handleMessagingNotification()
   ↓
3. ScamClassifierRouter.classify(text, channel)
   - detectLanguage() → DistilBERT or lazy IndicBERT
   - RiskEngine.calculate() → contextual rule score
   - Hybrid: ML × 0.60 + Rules × 0.40
   ↓
4. Route by finalScore:
   - < 0.40  → no action
   - ≥ 0.40  → SmsFraudNotifications (ALERTS_LOW channel)
   - ≥ 0.70  → SmsFraudNotifications (ALERTS_CRITICAL channel)
   ↓
5. MultiChannelCorrelationEngine.correlate*Event()
   - Query FraudDao for recent events in other channels
   - Match correlation strategies
   - If correlated → ThreatSessionEntity persisted
   ↓
6. FraudDao.insert*() → SQLCipher-encrypted Room DB
```

---

## Extension Guide

### Adding a new ML model

1. Place model and vocab under `assets/rakshakx_model/<model-name>/`
2. Update `model_config.json` with path, vocab, and max_seq_len
3. Implement a classifier wrapper extending the pattern in `DistilBertClassifier`
4. Integrate in `ScamClassifierRouter.classify()` as an additional routing step

### Adding a new detection signal

1. Implement an analyzer in the channel package (`email/analyzer/`, `web/analyzers/`, `call/callanalysis/`)
2. Contribute its score into `ScamClassifierRouter` via `ruleScore` adjustment or ML ensemble
3. Add keywords/patterns to `RiskEngine` if rule-based

### Adding a new UI screen

1. Create a `@Composable` function in `ui/screens/`
2. Add a `Screen` sealed class entry in `ui/navigation/NavHost.kt`
3. Add a `composable(Screen.YourScreen.route)` entry in the `NavHost`
4. Add to `hiddenRoutes` if it should not appear in bottom navigation

### Modifying Room schema

1. Change the entity class
2. Increment `@Database(version = N)` 
3. Add a `Migration(N-1, N)` object in `DatabaseFactory`
4. Run `./gradlew kspDebugKotlin` to regenerate and export schema snapshot to `schemas/`
