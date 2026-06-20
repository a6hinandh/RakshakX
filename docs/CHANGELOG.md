# Changelog

All notable changes to RakshakX are documented in this file.

---

## [2.0.1] — 2026-06-20

### Changed — UI/UX Overhaul

- **Splash Screen** — new slide-up animation for title and tagline; title uses `headlineLarge` for stronger presence; tagline changed from "Intelligent Protection" to "Your Shield. Your Device. Your Rules."
- **Onboarding Welcome Page** — redesigned circle glow effect with outer aura, thicker sweep gradient ring (2dp), and inner border glow using emerald/royal blue sweep gradients; welcome headline updated to "Your Shield. Your Device. Your Rules."
- **`PageHeader` composable** (`ui/components/Components.kt`) — new shared header component with RakshakX logo on the left of every page title, info button on the right corner that opens a dialog explaining the current page in simple language; replaces all per-screen header implementations
- **All 25 screens** — headers updated to use `PageHeader` with contextual info text; all page subtitles removed for cleaner UI
- **Footer** — changed from "RakshakX · On-device AI" to "RakshakX · v2.0.0" across all screens
- **Home Dashboard Tool Cards** — changed from horizontal Row layout (icon + text side by side) to vertical Column layout (icon on top, title + subtitle below) for better alignment
- **Shield Center Cards** — MITRE technique label and intercepted count now displayed on separate lines instead of a single cramped row
- **More Tools Menu Items** — badge now appears inline next to the title; improved vertical spacing

### Changed — Permission Flow

- **Accessibility Service** — no longer required during onboarding; removed from `minimumDashboardReady` in `PermissionManager`; users can skip during onboarding and are prompted with a dialog when they enable Web Shield in Shield Center
- **`PermissionManager.buildReadinessState()`** — `minimumDashboardReady` now requires only `corePermissionsGranted && notificationListenerEnabled && overlayEnabled` (accessibility removed)
- **`ShieldsControlScreen`** — added accessibility permission dialog that appears when user tries to enable Web Shield without accessibility service enabled

---

## [2.0.0] — 2026-06-17

### Added — Endpoint Security

- **`DeviceIntegrityScanner`** (`core/integrity/`) — root detection (su, Magisk, SuperSU, test-keys), debug/ADB/developer-options detection, security patch age check, screen lock and encryption state; weighted scoring with -40 for root to -5 for dev options
- **`SecurityPostureScore`** (`core/integrity/`) — composite A–F grade from device score (40%), network score (30%), and active threat count (30%)
- **`DeviceHealthScreen`** — animated Canvas arc score ring with grade-colored fill, staggered `FindingCard` list with severity-colored left borders, loading shimmer
- **`AppSecurityAuditor`** (`core/appsecurity/`) — iterates all installed packages via `PackageManager`; classifies install source (Play Store/Sideloaded/System/Unknown); computes `AppRiskLevel` (SAFE/LOW/MEDIUM/HIGH/CRITICAL)
- **`PermissionRiskModel`** (`core/appsecurity/`) — 40+ permission weights, three cluster bonuses (spyware +20, data harvesting +15, overlay attack +20)
- **`AppAuditScreen`** — stats row, filter chips, risk badge, top-3 dangerous permissions, install source chip per app

### Added — Network Security

- **`WifiSecurityAnalyzer`** (`core/network/`) — WPA3/WPA2/WPA/WEP/OPEN classification from `WifiManager` capabilities string, evil-twin detection (multiple BSSIDs same SSID), DNS hijack detection (gstatic.com IP range verification), captive portal detection (HTTP 204 check)
- **`LocalNetworkScanner`** (`core/network/`) — /24 subnet derivation from dhcpInfo, 254-host parallel `isReachable` probes, 12-port TCP scan, `/proc/net/arp` MAC resolution, 20-entry OUI map, risk classification for Telnet/SMB/VNC/RDP
- **`FirewallRule` + `FirewallRuleStore`** (`core/firewall/`) — per-app Wi-Fi/Mobile toggles stored as JSON in `EncryptedSharedPreferences` with `MasterKey`
- **`TrafficAnomalyDetector`** (`web/analyzers/`) — 5 behavioral detectors: beaconing (variance + inter-arrival), DGA (Shannon entropy >3.5), DNS tunneling (label length/query rate), cryptomining (24 pool domains), data exfiltration (>100 unique subdomains/5min)
- **`WifiAuditScreen`** — arc score ring, encryption badge, threat cards, recommendations
- **`FirewallScreen`** — per-app Wi-Fi/Mobile switch list with `EncryptedSharedPrefs` persistence
- **`NetworkScanScreen`** — pulsing radar animation, device cards with risky-port chips
- **`TrafficMonitorScreen`** — 5 detector status rows, anomaly cards with severity colors
- **`NetworkHubScreen`** — VPN status banner, 4 tool tiles, L1–L4 protection layer status

### Added — Threat Intelligence

- **`MitreAttackMapper`** (`core/correlation/`) — 15 ATT&CK for Mobile technique mappings across 13 tactics; `getAllTechniques()`, `mapThreat()`, `mapMultiple()`, `getTacticColor()`
- **`AttackMatrixScreen`** — tactic-grouped technique cards (detected=red border, monitored=grey), coverage stats (detected/total/rate%), tap-to-expand mitigation detail panel
- **`ForensicExporter`** (`core/forensics/`) — STIX 2.1 JSON bundle (threat-actor, identity, indicator, relationship, report objects), SHA-256 device fingerprint, `computeIntegrityHash()`, saves to `getExternalFilesDir()/rakshakx_forensics/`
- **`ForensicExportScreen`** — bundle ID + integrity hash (monospace), FileProvider share intent, cybercrime portal button
- **`ThreatAnalyticsScreen`** — 24h trend card, Canvas horizontal bar chart per channel, 24-column heatmap grid

### Added — Privacy & Vault

- **`TrackerDatabase`** (`core/privacy/`) — 50+ `TrackerSignature` entries across 6 categories (ANALYTICS, ADVERTISING, CRASH_REPORTING, FINGERPRINTING, SOCIAL, PROFILING); `detectTrackers()`, `getBlockDomains()`, `countByCategory()`
- **`PrivacyDashboardScreen`** — Canvas per-category progress bars, expandable app rows with tracker SDK list
- **`SecureVault`** (`core/vault/`) — AndroidKeyStore AES/GCM/NoPadding + EncryptedSharedPreferences two-layer storage; `VaultEntry` CRUD with category enum (PASSWORD/NOTE/RECOVERY_CODE/API_KEY/CREDIT_CARD/OTHER)
- **`VaultScreen`** — `combinedClickable` list, `AddEntryDialog` with `DropdownMenu`, `ViewEntryDialog` with reveal toggle, monospace font for sensitive values
- **`BreachChecker`** (`core/breach/`) — HIBP v3 email breach API + k-anonymity password range API via `HttpURLConnection` only; caches results in `SharedPreferences`
- **`BreachCheckScreen`** — email input with breach detail cards, k-anonymity password section

### Added — Hub Navigation

- **`ShieldHubScreen`** — on-device AI banner, detection module cards with MITRE technique IDs, shield action cards, permission status section
- **`MoreHubScreen`** — 3 sections: Device Security, Privacy, Intelligence; 9 navigation targets

### Changed

- **`NavHost.kt`** — complete rewrite; 5 bottom nav tabs (Home, Shield, Network, Threats, More); 22 total routes; `hiddenFromNav` set for all detail screens; `Icons.Filled.Hub` replaces `Icons.Filled.Lan`; `Icons.Filled.ShowChart` replaces `Icons.Filled.TrafficSharp`
- **`HomeDashboardScreen.kt`** — added `onNavigateToDeviceHealth` and `onNavigateToAttackMatrix` params; subtitle changed to "Mobile Threat Defense"; ATT&CK Matrix and Device Health tool cards added; section renamed "Security Tools"
- **`MitreAttackMapper.kt`** — added `getAllTechniques()` public method; added `RESOURCE_DEVELOPMENT` tactic
- Architecture diagram updated in README and ARCHITECTURE docs to include Endpoint Security and Threat Intelligence layers

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
