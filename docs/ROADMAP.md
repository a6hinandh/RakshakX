# RakshakX Roadmap

This document tracks completed work and planned future development.

---

## Completed — v2.0.0 (Cybersecurity Platform Expansion)

RakshakX expanded from a multi-channel fraud detector into a comprehensive mobile cybersecurity platform with endpoint security, network security, threat intelligence, and privacy protection capabilities.

### Endpoint Security ✅

| Feature | Status | Implementation |
|---------|--------|---------------|
| Device Integrity Scanner | ✅ Done | `core/integrity/DeviceIntegrityScanner.kt` — root/debug/ADB/patch/encryption checks |
| Security Posture Score | ✅ Done | `core/integrity/SecurityPostureScore.kt` — A–F composite grade (device 40% + network 30% + threats 30%) |
| App Security Auditor | ✅ Done | `core/appsecurity/AppSecurityAuditor.kt` — 40+ permission weights, install source, risk levels |
| Permission Risk Model | ✅ Done | `core/appsecurity/PermissionRiskModel.kt` — spyware/data-harvesting/overlay cluster detection |
| Device Health Screen | ✅ Done | `ui/screens/DeviceHealthScreen.kt` — animated arc ring, staggered finding cards |
| App Audit Screen | ✅ Done | `ui/screens/AppAuditScreen.kt` — filterable risk list, dangerous permissions, install source |

### Network Security ✅

| Feature | Status | Implementation |
|---------|--------|---------------|
| Wi-Fi Security Analyzer | ✅ Done | `core/network/WifiSecurityAnalyzer.kt` — WPA3/WPA2/WPA/WEP/OPEN, evil-twin, DNS hijack, captive portal |
| Local Network Scanner | ✅ Done | `core/network/LocalNetworkScanner.kt` — parallel 254-host probe, 12-port TCP scan, ARP/OUI lookup |
| Application Firewall | ✅ Done | `core/firewall/FirewallRuleStore.kt` — per-app Wi-Fi/Mobile rules, EncryptedSharedPreferences |
| Traffic Anomaly Detector | ✅ Done | `web/analyzers/TrafficAnomalyDetector.kt` — beaconing/DGA/tunneling/cryptomining/exfiltration |
| Wi-Fi Audit Screen | ✅ Done | `ui/screens/WifiAuditScreen.kt` |
| Firewall Screen | ✅ Done | `ui/screens/FirewallScreen.kt` |
| Network Scan Screen | ✅ Done | `ui/screens/NetworkScanScreen.kt` |
| Traffic Monitor Screen | ✅ Done | `ui/screens/TrafficMonitorScreen.kt` |
| Network Hub Screen | ✅ Done | `ui/screens/NetworkHubScreen.kt` — VPN status, tool grid, L1–L4 protection layers |

### Threat Intelligence ✅

| Feature | Status | Implementation |
|---------|--------|---------------|
| MITRE ATT&CK Mapper | ✅ Done | `core/correlation/MitreAttackMapper.kt` — 15 techniques, 13 tactics |
| ATT&CK Matrix Screen | ✅ Done | `ui/screens/AttackMatrixScreen.kt` — tactic groups, coverage stats, technique detail panel |
| Forensic Exporter (STIX 2.1) | ✅ Done | `core/forensics/ForensicExporter.kt` — STIX 2.1 bundles with integrity hash |
| Forensic Export Screen | ✅ Done | `ui/screens/ForensicExportScreen.kt` |
| Threat Analytics Screen | ✅ Done | `ui/screens/ThreatAnalyticsScreen.kt` — 24h trend, channel bar chart, heatmap |

### Privacy & Vault ✅

| Feature | Status | Implementation |
|---------|--------|---------------|
| Tracker Database | ✅ Done | `core/privacy/TrackerDatabase.kt` — 50+ signatures, 6 categories |
| Privacy Dashboard | ✅ Done | `ui/screens/PrivacyDashboardScreen.kt` — category progress bars, expandable app rows |
| Secure Vault | ✅ Done | `core/vault/SecureVault.kt` — AndroidKeyStore AES/GCM + EncryptedSharedPreferences |
| Vault Screen | ✅ Done | `ui/screens/VaultScreen.kt` — reveal toggle, category dropdown, monospace display |
| Breach Checker (HIBP) | ✅ Done | `core/breach/BreachChecker.kt` — HIBP v3 k-anonymity, HttpURLConnection only |
| Breach Check Screen | ✅ Done | `ui/screens/BreachCheckScreen.kt` |

### Navigation & UX ✅

| Feature | Status | Implementation |
|---------|--------|---------------|
| 5-tab navigation redesign | ✅ Done | Home / Shield / Network / Threats / More |
| Shield Hub | ✅ Done | `ui/screens/ShieldHubScreen.kt` — AI banner, MITRE IDs per module |
| More Hub | ✅ Done | `ui/screens/MoreHubScreen.kt` — 9 targets across 3 security sections |
| 25-screen navigation graph | ✅ Done | `ui/navigation/NavHost.kt` |
| v2.0.1 UI/UX overhaul | ✅ Done | `PageHeader` composable, all 25 screens standardized, permission flow streamlined |
| Cyber documentation | ✅ Done | README, ROADMAP, ARCHITECTURE, CHANGELOG updated to cybersecurity standards |

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

### Near-term (v2.1)

**Enable ProGuard/R8** — The single most impactful security improvement for a security app. Release builds need minification and obfuscation of all new security modules.

**Firewall enforcement** — Wire `FirewallRuleStore` rules into `FraudVpnService`'s UID-based packet filter to actually block traffic (not just store policies).

**Test coverage expansion** — Target areas:
- `DeviceIntegrityScanner` — mock Build.TAGS, su paths, patch date
- `PermissionRiskModel` — cluster detection, weight boundary conditions
- `WifiSecurityAnalyzer` — capabilities string parsing edge cases
- `TrafficAnomalyDetector` — Shannon entropy, DGA threshold tuning
- `BreachChecker` — k-anonymity prefix extraction, cache behavior
- `SecureVault` — encrypt/decrypt round-trip, IV uniqueness

**Branded notification icons** — Replace system drawables with RakshakX icons across all notification paths.

### Medium-term (v2.2)

**Behavioral app monitoring** — Extend `AppSecurityAuditor` to monitor runtime permission usage patterns (not just declared permissions) via `UsageStatsManager`, detecting apps that access camera/microphone more than expected.

**Network geo-blocking** — Add country-of-origin resolution to `TrafficAnomalyDetector` via offline IP-to-ASN database. Flag traffic to high-risk jurisdictions.

**MITRE technique expansion** — Add 10+ more ATT&CK for Mobile techniques covering Supply Chain, Firmware Exploitation, and SIM card attacks (T1474, T1422, T1533).

**HIBP email monitoring** — Periodic background check for new breaches affecting stored email addresses, with notification alerts.

**Wear OS companion** — Vibration alert on watch for CRITICAL threats, security score complication. Requires a `wearapp` Gradle module.

**INT4 model quantization** — Further reduce ONNX model size and inference latency beyond current INT8.

**Vosk on-demand download** — Move the bundled Vosk model (~50MB) to on-demand, reducing APK install size.

### Long-term (v3.x)

**Certificate Transparency monitoring** — Detect rogue TLS certificates issued for domains the user visits, using the public CT log stream.

**Additional Indic localization** — Extend string resources to all 11 supported detection languages (currently 4: Hindi, Tamil, Telugu, Kannada).

**Federated learning readiness** — Architecture to incorporate user corrections (false positive / false negative feedback) into model improvements without uploading raw data.

**NPCI UPI fraud integration** — Direct reporting path to the National Payments Corporation of India for confirmed UPI fraud cases.

**STIX sharing hub** — Opt-in mechanism to share anonymized STIX 2.1 bundles with community threat intelligence feeds, extending the app's data to benefit other security researchers.

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
