# RakshakX Stabilization Notes

## Phase 0 - Baseline Snapshot

### Command Results
- `./gradlew.bat :app:assembleDebug` -> PASS
- `./gradlew.bat :app:lintDebug` -> FAIL
- `./gradlew.bat :app:testDebugUnitTest` -> PASS

### Key Baseline Failures
- Lint reports permission-related errors; first reported at:
  - `app/src/main/java/com/security/rakshakx/call/services/foreground/RiskScanWorker.kt`
  - `MissingPermission` on `NotificationManagerCompat.notify(...)`
- Lint summary in run:
  - `11 errors, 131 warnings, 4 hints`

### Reproducible Issue List (Current)
1. Launch flow allows entering dashboard without proving required permissions are granted.
2. Dashboard channel statuses can appear active even when permissions are incomplete.
3. Boot path may restart monitoring-related work without revalidating current grants.
4. Notification posting paths are inconsistent in runtime `POST_NOTIFICATIONS` guarding.
5. Duplicate permission logic exists in multiple modules, causing readiness drift.
6. Lint fails on missing permission handling in at least one worker notification path.

## Implementation Progress

### Phase 1 - Permission Architecture Unification
- Added canonical readiness model in `permissions/PermissionManager.kt`:
  - `ReadinessState`
  - `getReadinessState(context)`
  - strict listener/accessibility detection via flattened `ComponentName` parsing
- Onboarding now gates dashboard entry until minimum requirements are satisfied.
- Dashboard channel cards now reflect real readiness (SMS/CALL/EMAIL) instead of hardcoded active states.
- Legacy call permission manager marked deprecated and aligned with app-level checks.
- Added unit tests for readiness matrix:
  - `app/src/test/java/com/security/rakshakx/permissions/PermissionManagerTest.kt`

### Phase 2 - Startup and Orchestration Contracts
- Added `startup/AppStartupCoordinator.kt` as startup contract authority.
- `MainActivity` now reconciles monitoring and SMS polling on app launch.
- `BootReceiver` now delegates to startup reconciliation instead of unconditional restarts.

### Phase 3 - Channel Integration
- Added `sms/SmsDeduplicationGuard.kt` with time-window dedupe.
- Applied dedupe across:
  - `sms/NotificationService.kt`
  - `sms/SmsReceiver.kt`
  - `sms/SmsPollingWorker.kt`
  - `call/services/receivers/SmsEventReceiver.kt`
- Added SMS/Email handler isolation in `NotificationService` with per-channel failure containment.
- Replaced call-screen hack placeholder by wiring `onStartHackathonMode` to real service start.
- Web channel status now requires both VPN running and accessibility readiness.

### Phase 4 - Service/Receiver Hardening
- Standardized alert/monitoring channel IDs toward app-wide values:
  - `rakshak_alerts`, `rakshak_fg`
- Added explicit notification permission guards in:
  - `call/services/foreground/RiskScanWorker.kt`
  - `sms/NotificationHelper.kt`
  - `web/notifications/VpnProtectionNotifier.kt`
- Fixed lint resource-type issue in `call/callanalysis/ui/PermissionSetupActivity.kt` by replacing raw numeric IDs with generated view IDs.

### Phase 5 - Test Harness
- Added instrumentation Compose test file:
  - `app/src/androidTest/java/com/security/rakshakx/onboarding/OnboardingScreenTest.kt`
- Kept baseline package/context instrumented test for smoke coverage.

## Latest Verification Snapshot
- `:app:testDebugUnitTest` -> PASS
- `:app:lintDebug` -> PASS
- `:app:assembleRelease` -> PASS
- `:app:connectedDebugAndroidTest` -> FAIL (environment): no connected devices detected in final run

