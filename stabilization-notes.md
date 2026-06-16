# RakshakX Stabilization Notes

Internal engineering log tracking build health across development phases.

---

## Phase 0 — Baseline Snapshot

### Build Results
- `./gradlew.bat :app:assembleDebug` → PASS
- `./gradlew.bat :app:lintDebug` → FAIL (11 errors, 131 warnings)
- `./gradlew.bat :app:testDebugUnitTest` → PASS

### Baseline Failures
1. Launch flow allowed entering dashboard without proving required permissions
2. Dashboard channel statuses appeared active even when permissions were incomplete
3. Boot path restarted monitoring-related work without revalidating current grants
4. Notification posting paths inconsistent in runtime `POST_NOTIFICATIONS` guarding
5. Duplicate permission logic in multiple modules causing readiness drift
6. Lint failure: missing permission handling in `RiskScanWorker.kt` notification path

---

## Phase 1 — Permission Architecture Unification

- Added canonical readiness model in `permissions/PermissionManager.kt`
  - `ReadinessState` enum
  - `getReadinessState(context)` with strict listener/accessibility detection
- Onboarding now gates dashboard until minimum permissions satisfied
- Dashboard channel cards reflect real readiness instead of hardcoded active states
- Legacy call permission manager deprecated and aligned with app-level checks
- Added unit tests: `permissions/PermissionManagerTest.kt`

---

## Phase 2 — Startup and Orchestration

- Added `startup/AppStartupCoordinator.kt` as startup contract authority
- `MainActivity` reconciles monitoring and SMS polling on launch
- `BootReceiver` delegates to startup reconciliation instead of unconditional service restarts

---

## Phase 3 — Channel Integration

- Added `sms/SmsDeduplicationGuard.kt` with time-window deduplication
- Applied dedup across all three SMS ingress paths
- Added SMS/Email handler isolation in NotificationService with per-channel failure containment
- Replaced call-screen hack placeholder by wiring `onStartHackathonMode` to real service start
- Web channel status now requires both VPN running and accessibility readiness

---

## Phase 4 — Service/Receiver Hardening

- Standardized notification channel IDs: `rakshak_alerts`, `rakshak_fg`
- Added explicit `POST_NOTIFICATIONS` permission guards in `RiskScanWorker`, `NotificationHelper`, `VpnProtectionNotifier`
- Fixed lint resource-type issue in `PermissionSetupActivity` (replaced raw numeric IDs with generated view IDs)

---

## Phase 5 — Test Harness

- Added `OnboardingScreenTest.kt` instrumented Compose test
- Kept baseline package/context instrumented test for smoke coverage

---

## Phase 6 — Roadmap Implementation (v1.1.0)

### Changes

**Phase 1 of roadmap (core engine):**
- Deleted `WhisperLiteStub.kt`, `WhisperLiteTranscriber.kt`; all transcription via Vosk
- Added file-based `transcribe(audioPath)` and `isModelAvailable()` to `VoskTranscriber`
- Rewrote `MultiChannelCorrelationEngine` with 5 correlation strategies and new `CorrelationType` enum
- Expanded `FraudDao` with queries for time-range and phone-based lookups
- Rewrote `RiskEngine` with contextual scoring, sender reputation, time weighting

**Phase 2 of roadmap (features):**
- Redesigned `activity_call_overlay.xml` and `OverlayBubbleService` with premium UI and quick action buttons
- Added 3 new notification channels; rewrote `SmsFraudNotifications` with notification grouping
- Created `ThreatIntelligenceManager`, `ThreatIntelScreen`
- Created `SecurityDigestWorker` scheduled via WorkManager

**Phase 3 of roadmap (new features):**
- Extended `RakshakNotificationListenerService` for WhatsApp/Telegram/Signal interception
- Added UPI pattern detection in notification listener and RiskEngine
- Created `ScamCallDatabase`, `ModelUpdateManager`, `FamilyProtectionManager`
- Created `FamilyProtectionScreen`, `ThreatIntelScreen`

**Phase 4 of roadmap (platform):**
- Created `SecurityWidgetProvider`, `widget_security_status.xml`, `widget_security_info.xml`
- Added widget background drawables in `drawable/` and `drawable-v26/`
- Created Hindi, Tamil, Telugu, Kannada string resource files
- Created `.github/workflows/android-ci.yml`
- Implemented lazy IndicBERT loading in `ScamClassifierRouter`

### Bugs Fixed During Implementation
- Nullable `sourceSms` crash in `UrlScanActivity` (email-web correlation path)
- Missing coroutine imports in `OverlayBubbleService` for Report Number action

---

## Current Build Status — v1.1.0

- `./gradlew :app:assembleDebug` → **PASS**
- `./gradlew :app:testDebugUnitTest` → **PASS**
- `./gradlew :app:lintDebug` → **PASS** (deprecation warnings only, no errors)
- `./gradlew :app:connectedDebugAndroidTest` → REQUIRES CONNECTED DEVICE

### Remaining Lint Warnings (Non-blocking)
- `TelecomManager.endCall()` deprecated API (no suitable replacement for API 26 support floor)
- `AudioManager.isSpeakerphoneOn` deprecated
- `VIBRATOR_SERVICE` deprecated static field
- Multiple `Icons.Filled.ArrowBack` → `Icons.AutoMirrored.Filled.ArrowBack` in Compose screens
- `Window.statusBarColor` / `navigationBarColor` deprecated in `Theme.kt`

None of these are build errors. They can be addressed incrementally.

---

## Remaining Technical Debt

See [ROADMAP.md](docs/ROADMAP.md#technical-debt--remaining) for the current debt list and priorities.
