# RakshakX — On-Device Mobile Cybersecurity Platform

**Real-time, privacy-preserving threat detection and response for Android**

RakshakX is a multi-layered mobile cybersecurity platform that provides endpoint protection across five attack surfaces — SMS, voice calls, email, web browsing, and instant messaging — using on-device machine learning, behavioral analysis, network-level traffic inspection, and cross-channel threat correlation. Every computation runs locally; zero user data leaves the device.

---

## Cybersecurity Capabilities

### 1. Multi-Vector Threat Detection

RakshakX defends against the full spectrum of social engineering and phishing attacks targeting mobile users:

| Attack Vector | Threat Types Detected | Detection Technique |
|---------------|----------------------|---------------------|
| **SMS (Smishing)** | Phishing links, OTP harvesting, UPI fraud, fake banking alerts, prize scams, job/investment fraud | Hybrid NLP classification (DistilBERT/IndicBERT) + contextual rule engine with sender reputation |
| **Voice Calls (Vishing)** | Tech support scams, law enforcement impersonation, banking fraud, credential harvesting, remote access social engineering | Real-time speech-to-text (Vosk ASR) + fraud intent classification + pre-call number reputation screening |
| **Email (Phishing)** | Credential phishing, spear phishing, BEC (business email compromise), malicious attachments, URL obfuscation, homoglyph attacks | Multi-signal pipeline: URL reputation, intent analysis, obfuscation detection, attachment risk scoring |
| **Web (Drive-by / Malicious URLs)** | Phishing sites, malware distribution domains, typosquatting, redirect chain attacks, malicious QR codes | VPN-based DNS sinkholing + domain reputation analysis + TLS metadata inspection + redirect chain tracking |
| **Messaging (WhatsApp/Telegram/Signal)** | Forwarded scam chains, UPI collect request fraud, social engineering via messaging platforms | Notification interception + ML classification + UPI deep-link pattern detection |

### 2. Cross-Channel Threat Correlation (MITRE ATT&CK Aligned)

The `MultiChannelCorrelationEngine` is RakshakX's differentiating capability — it detects **coordinated multi-stage attacks** that single-channel filters miss. These map to real-world attack kill chains:

| Correlation Strategy | Kill Chain Pattern | Time Window | Risk Escalation |
|---------------------|--------------------|-------------|-----------------|
| `SMS_WEB_URL` | Smishing → victim visits phishing site | 1 hour | +0.25 |
| `EMAIL_WEB_URL` | Phishing email → victim visits spoofed portal | 1 hour | +0.25 |
| `CALL_SMS_PHONE` | Vishing call → OTP-harvesting SMS from same number | 15 minutes | +0.35 |
| `PHONE_MULTI_CHANNEL` | Persistent threat actor across call + SMS + email | 24 hours | +0.55 |
| `TEMPORAL_BURST` | Coordinated blitz across 2+ channels in rapid succession | 15 minutes | +0.15 |

Example: A scammer calls pretending to be from a bank → hangs up → sends an SMS with a "verification link" → the user clicks the link and visits a phishing site. RakshakX detects each event independently, then the correlation engine links all three via phone number and URL domain matching, creating a `ThreatSessionEntity` that escalates the combined risk score and triggers a critical alert.

### 3. Network Security Layer (Local VPN)

`FraudVpnService` implements a **DNS-level network security layer** via Android's VPN API:

```
App DNS request → TUN interface (10.0.0.1)
  → DnsVpnRelay intercepts query
  → PacketParser extracts domain
  → DomainRiskAnalyzer checks:
      ├─ Known malicious domain database
      ├─ Typosquatting detection
      ├─ Suspicious TLD analysis
      ├─ Domain age heuristics
      └─ TLS certificate metadata
  → ThreatBlockingEngine: ALLOW / WARN / BLOCK
  → DNS forwarded to upstream resolver
```

Design decision: **no MITM.** The VPN does not install a custom CA or decrypt TLS — this would compromise the security of every HTTPS connection on the device. Instead, it analyzes DNS metadata, which is sufficient for domain-level threat intelligence without breaking encryption guarantees.

### 4. On-Device ML Inference Pipeline

All machine learning runs locally using ONNX Runtime — no cloud API calls, no data exfiltration risk:

| Model | Architecture | Purpose | Runtime |
|-------|-------------|---------|---------|
| **DistilBERT** | `distilbert-base-uncased`, fine-tuned, INT8 quantized | English scam/phishing NLP classification | ONNX Runtime 1.19.2 |
| **IndicBERT** | `ai4bharat/IndicBERT`, fine-tuned, INT8 quantized | 11 Indian language scam/phishing classification | ONNX Runtime 1.19.2 (lazy-loaded) |
| **Vosk ASR** | Lightweight English acoustic model | Real-time call transcription (16kHz PCM → text) | Vosk 0.3.38 |
| **AiThreatScorer** | TFLite/ONNX web fraud model | Domain + page-level threat scoring | TensorFlow Lite 2.9.0 |

**Hybrid Scoring Formula:**
```
finalScore = (ML_confidence × 0.60) + (RuleEngine_score / 100 × 0.40)

Classification:
  < 0.40  →  SAFE          (no action)
  0.40–0.69  →  SUSPICIOUS  (warning notification)
  ≥ 0.70  →  SCAM          (critical alert, blocking options)
```

The 60/40 ML/rules split is deliberate — pure ML models are vulnerable to adversarial evasion (crafted messages that bypass learned patterns). The rule-based `RiskEngine` provides a safety net with keyword matching, sender reputation tracking, time-of-day weighting, and combination amplification that catches threats even when ML is fooled.

**Language Detection:** Zero-dependency Unicode block analysis routes text to the correct model:
- Counts characters per script (Devanagari, Tamil, Telugu, Kannada, Malayalam, Bengali, Gujarati, Gurmukhi, Odia, Arabic/Urdu)
- If any Indic script ≥ 15% of letters OR ≥ 6 characters → IndicBERT
- Otherwise → DistilBERT
- Low-confidence DistilBERT result with detectable Indic script → automatic fallback to IndicBERT

### 5. Encrypted Data-at-Rest

All threat data is persisted in **SQLCipher-encrypted Room databases** (AES-256-CBC):

| Layer | Technology | Purpose |
|-------|-----------|---------|
| Database encryption | SQLCipher 4.6.1 | AES-256 encryption of all Room databases |
| Key management | Android Keystore + `androidx.security.crypto` | Hardware-backed encryption key derivation |
| File encryption | `EncryptedFile` (AES-256-GCM) | Sensitive threat logs on disk |
| Settings | `EncryptedSharedPreferences` | User preferences and configuration |

The encryption key is derived via the Android Keystore system, making it hardware-protected on devices with a TEE (Trusted Execution Environment) or StrongBox.

### 6. Threat Intelligence Network

`ThreatIntelligenceManager` implements **privacy-preserving threat sharing** with differential privacy:

- **Shared:** SHA-256 hashed phone numbers and domains only (irreversible one-way hash)
- **Never shared:** Raw identifiers, message content, call transcripts, contact lists, location, device info
- **Timestamps:** Rounded to nearest hour for k-anonymity
- **Strictly opt-in:** Disabled by default, user controls via `ThreatIntelScreen`

### 7. Real-Time Call Protection

During active phone calls, RakshakX provides a live security overlay:

```
Incoming call → CallStateMonitor (PHONE_STATE broadcast)
  → PreActionDecisionEngine: check ScamCallDatabase for known scam numbers
  → CallRecordingService: capture audio (RECORD_AUDIO, foreground service)
  → VoskTranscriber: streaming speech-to-text (16kHz PCM → JSON transcript)
  → FraudInferenceEngine + FraudIntentClassifier: classify transcript segments
  → OverlayBubbleService: floating UI overlay (SYSTEM_ALERT_WINDOW)
      ├─ Live risk meter (color-coded: green/amber/red)
      ├─ Contextual fraud phrase warnings
      └─ Quick actions: End Call, Report Number, Mark Safe
  → FraudDao.insertCall(): persist to encrypted database
  → MultiChannelCorrelationEngine.correlateCallEvent(): cross-channel check
```

**Fraud intent categories:** Financial fraud, tech support scam, prize/lottery, urgent action demand, credential harvesting, law enforcement impersonation.

### 8. QR Code & URL Security Scanner

`QrScannerActivity` (CameraX + ML Kit Barcode) and `UrlScanActivity` provide:
- Real-time QR code scanning with domain reputation check before navigation
- Context menu integration (`ACTION_PROCESS_TEXT`) — long-press any URL in any app to scan
- Redirect chain analysis for shortened/obfuscated URLs
- Whitelisting for known-safe domains (government portals, major banks)

### 9. Family Protection Mode

`FamilyProtectionManager` provides role-based security for non-technical family members:

| Role | Capabilities |
|------|-------------|
| **Admin** | Full access, manages members and settings |
| **Elder** | Simplified UI with large text, receives extra guidance, alerts forwarded to admin |
| **Child** | Restricted controls, all critical alerts forwarded to admin |
| **Self** | Standard single-user mode |

### 10. Device Integrity & Health Scoring

`DeviceIntegrityScanner` performs a comprehensive security posture evaluation of the Android device itself:

- **Root detection** — checks `/system/bin/su`, `/system/xbin/su`, Magisk paths, SuperSU artifacts, `Build.TAGS` for test-keys
- **Weighted risk scoring** — deductions for root (-40), debug build (-10), ADB enabled (-10), developer options (-5), unknown sources (-10), security patch >90 days old (-15), no screen lock (-15), no encryption (-10), no Play Protect (-10)
- **Composite Security Posture Score** — `SecurityPostureScore` combines device score (40%), network score (30%), and active threat count (30%) into a letter grade (A–F)
- **`DeviceHealthScreen`** — animated arc score ring with grade-colored fill, staggered finding cards with severity-colored borders

### 11. App Security Auditing

`AppSecurityAuditor` iterates all installed packages via `PackageManager` and evaluates each app against a multi-factor risk model:

- **`PermissionRiskModel`** — 40+ permission weights (CAMERA:15, RECORD_AUDIO:15, BIND_ACCESSIBILITY_SERVICE:30, PROCESS_OUTGOING_CALLS:20) with spyware cluster detection (CAMERA+RECORD_AUDIO+INTERNET = +20 bonus), data harvesting cluster, and overlay attack cluster
- **Install source classification** — Play Store / Sideloaded / System / Unknown via installer package name
- **Risk levels** — SAFE / LOW / MEDIUM / HIGH / CRITICAL based on permission score, sideload status, device admin status
- **`AppAuditScreen`** — filterable list with risk badge, top-3 dangerous permissions, and install source chip per app

### 12. Wi-Fi Security Analysis

`WifiSecurityAnalyzer` evaluates the current Wi-Fi connection for encryption quality and active attacks:

- **Encryption classification** — parses `WifiManager` capabilities string for WPA3 / WPA2 / WPA / WEP / OPEN; scored 100/85/50/20/0
- **Evil-twin detection** — identifies multiple BSSIDs sharing the same SSID at strong signal strength (rogue AP attack)
- **DNS hijack detection** — resolves `connectivitycheck.gstatic.com` and verifies the response IP falls within known Google ranges; any other IP indicates DNS manipulation
- **Captive portal detection** — HTTP 204 check to identify intercepting portals before sensitive operations
- **`WifiAuditScreen`** — arc score ring, encryption badge, threat card list, and specific remediation recommendations

### 13. Application Firewall

`FirewallRuleStore` backed by `EncryptedSharedPreferences` provides per-app network policy enforcement:

- **Per-app Wi-Fi / Mobile data toggles** — rules stored as JSON in `EncryptedSharedPreferences` (AES-256-GCM, `MasterKey` via AndroidKeyStore)
- **`FirewallRule`** data class — packageName, allowWifi, allowMobile, blockedDomains list, enabled flag
- **`FirewallScreen`** — non-system app list from `PackageManager`, switch toggles per app, real-time rule persistence
- Designed to feed into `FraudVpnService`'s UID-based packet filtering for enforcement at the network layer

### 14. Local Network Scanner

`LocalNetworkScanner` maps the local Wi-Fi subnet for security threats:

- **Parallel host discovery** — derives /24 subnet from `WifiManager.dhcpInfo`, fans 254 concurrent `isReachable(200ms)` probes via `async`/`awaitAll`
- **TCP port scanning** — 12 ports per discovered host: 21 (FTP), 22 (SSH), 23 (Telnet), 80, 443, 445 (SMB), 1883 (MQTT), 5683 (CoAP), 5900 (VNC), 3389 (RDP), 8080, 8443
- **MAC-to-vendor lookup** — reads `/proc/net/arp` for MAC addresses, maps against 20-entry OUI table
- **Risk classification** — Telnet=HIGH_RISK (unencrypted remote shell), SMB/VNC/RDP=SUSPICIOUS (lateral movement surfaces)
- **`NetworkScanScreen`** — pulsing radar animation during scan, device cards with colored risky-port chips

### 15. Traffic Anomaly Detection

`TrafficAnomalyDetector` (in `web/analyzers/`) performs behavioral analysis on DNS query streams intercepted by the VPN:

- **C2 Beaconing** — inter-arrival variance <5s² with <2min average interval; indicates automated periodic callback to a command-and-control server
- **DGA Domain Detection** — Shannon entropy >3.5, consonant run >8, digit ratio >0.3, domain length >15; characteristic of Domain Generation Algorithm malware
- **DNS Tunneling** — label length >30 chars (HIGH) or >50 unique queries/min (MEDIUM); data exfiltration via DNS sublabel encoding
- **Cryptomining** — 24 known mining pool domains (pool.supportxmr.com, xmrpool.eu, etc.)
- **Data Exfiltration** — >100 unique subdomains per apex domain within a 5-minute window
- **`TrafficMonitorScreen`** — five detector status rows with active/severity indicators, anomaly cards with technique description

### 16. Privacy Dashboard & Tracker Detection

`TrackerDatabase` contains 50+ tracker signatures mapped to `TrackerCategory` (ANALYTICS, ADVERTISING, CRASH_REPORTING, FINGERPRINTING, SOCIAL, PROFILING):

- Detects trackers in installed apps by matching package names against known tracker SDKs (Google Analytics, Facebook Ads, AppsFlyer, Adjust, Branch, Firebase, Crashlytics, etc.)
- Canvas-rendered per-category progress bars showing tracker prevalence
- App-level drill-down listing specific tracker SDKs embedded per app
- **`PrivacyDashboardScreen`** — expandable app rows with category breakdown and block-domain list

### 17. Secure Credential Vault

`SecureVault` provides on-device encrypted credential storage using two independent security layers:

- **Transport encryption** — AndroidKeyStore AES/GCM/NoPadding, fresh IV prepended to every ciphertext, Base64 output
- **Storage layer** — `EncryptedSharedPreferences` with `MasterKey` (hardware-backed on TEE/StrongBox devices)
- **`VaultEntry`** — id, title, content, category (`VaultCategory`: PASSWORD / NOTE / RECOVERY_CODE / API_KEY / CREDIT_CARD / OTHER), timestamps
- **`VaultScreen`** — `combinedClickable` list with long-press reveal, `AddEntryDialog` with category dropdown, monospace font for sensitive values

### 18. Data Breach Detection (k-Anonymity HIBP)

`BreachChecker` integrates with the Have I Been Pwned v3 API using **k-anonymity** — the full hash or email never leaves the device:

- **Email breach check** — queries `/breachedaccount/{email}` with only the SHA-1 prefix sent; 404=clean, 200=returns breach list
- **Password hash check** — sends only the first 5 hex chars of the SHA-1 password hash to `/range/{prefix}`; compares the returned hash suffixes locally
- **Response caching** — results cached in `SharedPreferences` to avoid repeat API calls
- **`HttpURLConnection` only** — zero external HTTP library dependencies; no OkHttp/Retrofit
- **`BreachCheckScreen`** — email input with breach detail cards, password section with k-anonymity guarantee display

### 19. MITRE ATT&CK Mobile Matrix

`MitreAttackMapper` maps detected threats to the MITRE ATT&CK for Mobile framework:

| Technique ID | Name | Tactic | Detection Source |
|-------------|------|--------|-----------------|
| T1660 | Phishing (SMS) | Initial Access | SMS channel |
| T1660.001 | Phishing: Spearphishing Voice | Initial Access | Call channel |
| T1566 | Phishing (Email) | Initial Access | Email channel |
| T1659 | Content Injection | Initial Access | Web channel |
| T1417 | Input Capture: Keylogging | Collection | App audit |
| T1571 | Non-Standard Port | C2 | Beaconing detector |
| T1496 | Resource Hijacking | Impact | Cryptomining detector |
| T1437 | Application Layer Protocol | Exfiltration | Exfil detector |
| T1584.002 | Compromise Infrastructure: DNS | Resource Dev | Wi-Fi audit |
| T1404 | Exploitation for Priv Esc | Privilege Escalation | Device integrity |
| T1476 | Deliver Malicious App | Initial Access | App audit |
| T1418 | Software Discovery | Discovery | App audit |

- **`AttackMatrixScreen`** — tactic-grouped technique cards (red border = detected, grey = monitored), coverage stats, tap-to-expand detail panel with mitigations

### 20. Threat Analytics Dashboard

`ThreatAnalyticsScreen` visualizes the historical threat dataset:

- **24-hour trend card** — rolling hourly event count for the past 24 hours
- **Channel breakdown bar chart** — Canvas `drawRoundRect` horizontal bars per channel (SMS/Call/Email/Web/Messaging) normalized to max count
- **24-hour heatmap grid** — 24-column Canvas grid where each cell's alpha encodes threat density for that hour, enabling pattern recognition of attack timing

### 21. Forensic Export (STIX 2.1)

`ForensicExporter` generates industry-standard threat intelligence bundles:

- **STIX 2.1 JSON format** — threat-actor, identity, indicator, relationship, and report objects with proper `spec_version: "2.1"` headers
- **Device fingerprint** — SHA-256 of `ANDROID_ID + appVersion`; included in every bundle as the reporting identity
- **Integrity hash** — `computeIntegrityHash()` produces a SHA-256 of the entire bundle for tamper evidence
- **File output** — saves to `getExternalFilesDir()/rakshakx_forensics/` for direct file access without FileProvider
- **`ForensicExportScreen`** — bundle ID + integrity hash display (monospace font), FileProvider sharing intent, cybercrime portal button

---

## System Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│                         PRESENTATION LAYER                           │
│  Jetpack Compose (Material3)  │  Glassmorphism Dark Theme            │
│  5-Tab Navigation (Home/Shield/Network/Threats/More)                 │
│  Haptic Feedback System       │  Animated Onboarding (Lottie)        │
│  PageHeader component style   │  Glassmorphism surface components    │
│  25 Screens across 5 security domains                                │
├──────────────────────────────────────────────────────────────────────┤
│                        ORCHESTRATION LAYER                           │
│  FraudMonitoringForegroundService  │  AppStartupCoordinator          │
│  MultiChannelCorrelationEngine     │  ScamClassifierRouter            │
│  SecurityDigestWorker              │  ModelUpdateManager (OTA)        │
│  PreActionDecisionEngine           │  ScamAlertManager                │
├──────────┬──────────┬──────────┬───────────┬────────────────────────┤
│   SMS    │  EMAIL   │   CALL   │    WEB    │  MESSAGING             │
│ SmsScan  │ EmailTh  │ Vosk+   │ VPN+DNS   │  WhatsApp/Telegram     │
│ Dedup    │ Pipeline │ Overlay │ TLS+QR    │  Signal/UPI            │
│ 3xIngest │ Analyzer │ CallRec │ DomainRep │  ForwardDetect         │
├──────────┴──────────┴──────────┴───────────┴────────────────────────┤
│                      ENDPOINT SECURITY LAYER  (NEW v2.0)            │
│  DeviceIntegrityScanner (root/debug/patch)  │  SecurityPostureScore  │
│  AppSecurityAuditor (40+ permission weights) │  PermissionRiskModel   │
│  WifiSecurityAnalyzer (WPA3/evil-twin/DHNS) │  LocalNetworkScanner   │
│  FirewallRuleStore (EncryptedSharedPrefs)   │  TrafficAnomalyDetector│
├──────────────────────────────────────────────────────────────────────┤
│                       THREAT INTELLIGENCE LAYER  (NEW v2.0)         │
│  MitreAttackMapper (15 techniques, 13 tactics)  │  ForensicExporter  │
│  TrackerDatabase (50+ signatures, 6 categories) │  STIX 2.1 export  │
│  BreachChecker (HIBP v3, k-anonymity)           │  SecureVault       │
├──────────────────────────────────────────────────────────────────────┤
│                          ML / AI LAYER                               │
│  DistilBERT (ONNX, English)   │  IndicBERT (ONNX, 11 Indic langs)  │
│  Vosk ASR (call transcription) │  AiThreatScorer (web fraud)        │
│  FraudIntentClassifier          │  RiskEngine (contextual rules)     │
├──────────────────────────────────────────────────────────────────────┤
│                          SECURITY LAYER                              │
│  SQLCipher (AES-256-CBC)       │  Android Keystore (TEE/StrongBox)  │
│  EncryptedFile (AES-256-GCM)   │  SecureVault (AES/GCM/NoPadding)  │
│  EncryptedSharedPreferences     │  SHA-256 differential privacy      │
├──────────────────────────────────────────────────────────────────────┤
│                           DATA LAYER                                 │
│  Room (FraudDao, ThreatDao)    │  ThreatSessionEntity               │
│  ThreatIntelligenceManager     │  ScamCallDatabase                   │
│  FamilyProtectionManager       │  SettingsStore (StateFlow)          │
└──────────────────────────────────────────────────────────────────────┘
```

### Detection Pipeline

```
1. INGRESS ─────── NotificationListenerService / BroadcastReceiver / VPN / Polling
                        │
2. DEDUPLICATION ── SmsDeduplicationGuard (time-window hash, prevents triple-processing)
                        │
3. ROUTING ──────── Package name → channel dispatch (SMS / Email / Call / Web / Messaging)
                        │
4. ANALYSIS ─────── Channel-specific detector → ScamClassifierRouter.classify(text, channel)
                        │
                   ┌────┴────┐
                   │         │
5. ML PATH ──── DistilBERT   IndicBERT       ← language detection routing
                or IndicBERT                  ← lazy-loaded on first Indic text
                   │         │
6. RULE PATH ── RiskEngine.calculate(text, sender, context)
                   │         │
                   └────┬────┘
                        │
7. HYBRID SCORE ── finalScore = ML × 0.60 + Rules × 0.40
                        │
8. ALERT ──────── ScamAlertManager → severity-grouped notification channels
                        │
9. PERSISTENCE ── FraudDao.insert*() → SQLCipher-encrypted Room database
                        │
10. CORRELATION ── MultiChannelCorrelationEngine → ThreatSessionEntity
                        │
11. RESPONSE ──── Block / Warn / Report / Auto-silence / Family alert forwarding
```

---

## Tech Stack

| Category | Technology | Version |
|----------|-----------|---------|
| Language | Kotlin | 2.2.10 |
| UI Framework | Jetpack Compose + Material3 | BOM 2026.02.01 |
| Build System | Gradle Kotlin DSL + KSP | 9.4.1 / 2.3.0 |
| ML Inference | ONNX Runtime (on-device NLP) | 1.19.2 |
| ML Inference | TensorFlow Lite (web fraud) | 2.9.0 |
| Speech-to-Text | Vosk (on-device ASR) | 0.3.38 |
| Database | Room + SQLCipher | 2.7.2 / 4.6.1 |
| Crypto | AndroidX Security Crypto | Latest |
| Camera | CameraX (QR scanning) | 1.4.1 |
| Barcode | ML Kit Barcode Scanning | Latest |
| Background | WorkManager | 2.10.1 |
| Animation | Lottie Compose | 6.6.6 |
| Min SDK | Android 8.0 | API 26 |
| Target SDK | Android 15 | API 36 |
| JDK | Java 21 | 21 |

---

## Project Structure

```
RakshakX/
├── app/src/main/java/com/security/rakshakx/
│   ├── call/                     # Call channel — 54 files
│   │   ├── callanalysis/         # VoskTranscriber, CallStateMonitor, OverlayBubbleService
│   │   ├── core/                 # Orchestrator, EncryptionManager, DatabaseFactory
│   │   ├── services/             # FraudMonitoringForegroundService, CallRecordingService
│   │   ├── ai/                   # CallScamDetector, FraudInferenceEngine, ModelLoader
│   │   └── data/                 # CallRecord, CallRepository, BlockedNumbersRepository
│   ├── sms/                      # SMS channel — 7 files
│   │   ├── SmsScamDetector.kt    # ML classification entry point
│   │   ├── SmsReceiver.kt        # SMS_RECEIVED broadcast + correlation trigger
│   │   ├── SmsPollingWorker.kt   # Fallback inbox polling (WorkManager)
│   │   ├── SmsDeduplicationGuard.kt  # Time-window hash deduplication
│   │   └── RiskEngine.kt         # Contextual rule scoring (8 categories, multilingual)
│   ├── email/                    # Email channel — 17 files
│   │   ├── analyzer/             # URL, intent, obfuscation, attachment analyzers
│   │   ├── database/             # ThreatDatabase (Room), ThreatEntity, ThreatDao
│   │   ├── pipeline/             # EmailThreatPipeline (orchestrator)
│   │   ├── scoring/              # ThreatCorrelationEngine
│   │   └── EmailScamDetector.kt  # ML classification entry point
│   ├── web/                      # Web channel — 37 files
│   │   ├── services/             # FraudVpnService, DnsVpnRelay, AccessibilityMonitorService
│   │   ├── analyzers/            # DomainRisk, ScamLanguage, BrowserNetworkCorrelation
│   │   ├── extractors/           # PacketParser, TlsMetadataExtractor, RedirectChainTracker
│   │   ├── ai/                   # AiThreatScorer, ModelManager, OnDeviceFraudModel
│   │   ├── storage/              # EncryptedStorageManager, CryptoKeyManager
│   │   └── ui/                   # QrScannerActivity, UrlScanActivity, VpnDashboardScreen
│   ├── integration/              # Shared ML layer — 6 files
│   │   ├── ScamClassifierRouter.kt   # Central routing: language detect → model select → hybrid score
│   │   ├── DistilBertClassifier.kt   # ONNX inference + WordPiece tokenizer (eager load)
│   │   ├── IndicBertClassifier.kt    # ONNX inference + SentencePiece tokenizer (lazy load)
│   │   ├── ModelResult.kt            # Classification output data class
│   │   └── ScamAlertManager.kt       # Severity-based alert routing
│   ├── core/                     # Cross-cutting concerns
│   │   ├── correlation/          # MultiChannelCorrelationEngine (5 strategies) + MitreAttackMapper
│   │   ├── integrity/            # DeviceIntegrityScanner, SecurityPostureScore  (NEW)
│   │   ├── appsecurity/          # AppSecurityAuditor, PermissionRiskModel        (NEW)
│   │   ├── network/              # WifiSecurityAnalyzer, LocalNetworkScanner      (NEW)
│   │   ├── firewall/             # FirewallRule, FirewallRuleStore                (NEW)
│   │   ├── privacy/              # TrackerDatabase (50+ signatures)               (NEW)
│   │   ├── breach/               # BreachChecker (HIBP v3 k-anonymity)            (NEW)
│   │   ├── vault/                # SecureVault (AES/GCM + EncryptedSharedPrefs)  (NEW)
│   │   ├── forensics/            # ForensicExporter (STIX 2.1)                    (NEW)
│   │   ├── threatintel/          # ThreatIntelligenceManager (SHA-256 sharing)
│   │   ├── family/               # FamilyProtectionManager (role-based)
│   │   ├── callerid/             # ScamCallDatabase (pre-call screening)
│   │   ├── modelupdate/          # ModelUpdateManager (OTA with rollback)
│   │   └── SettingsStore.kt      # Per-channel enable/disable (StateFlow)
│   ├── data/                     # Unified persistence
│   │   ├── entities/FraudEntities.kt  # 5 Room entities (SMS, Call, Email, Web, ThreatSession)
│   │   ├── dao/FraudDao.kt            # Multi-channel queries + correlation lookups
│   │   └── repository/FraudRepository.kt
│   ├── notifications/            # Alert system
│   │   ├── RakshakNotificationListenerService.kt  # Unified listener (20+ app packages)
│   │   ├── RakshakNotificationChannels.kt         # CRITICAL / SUSPICIOUS / DIGEST channels
│   │   ├── SecurityDigestWorker.kt                # Daily 24h threat summary
│   │   └── receivers/NotificationActionReceiver.kt  # Block / Report / Mark Safe actions
│   ├── permissions/              # PermissionManager, readiness state model
│   ├── onboarding/               # Progressive permission setup (6-step wizard)
│   ├── startup/                  # AppStartupCoordinator (boot sequence)
│   ├── widget/                   # SecurityWidgetProvider (home screen)
│   └── ui/                       # Compose UI layer
│       ├── screens/              # 25 screens across 5 security domains
│       │   ├── HomeDashboardScreen.kt, ThreatLogsScreen.kt, CorrelationScreen.kt
│       │   ├── ShieldsControlScreen.kt, NetworkHubScreen.kt, MoreHubScreen.kt
│       │   ├── DeviceHealthScreen.kt, AppAuditScreen.kt   (endpoint security)
│       │   ├── WifiAuditScreen.kt, FirewallScreen.kt      (network security)
│       │   ├── NetworkScanScreen.kt, TrafficMonitorScreen.kt
│       │   ├── PrivacyDashboardScreen.kt, PrivacyScreen.kt, VaultScreen.kt
│       │   ├── ThreatAnalyticsScreen.kt, ForensicExportScreen.kt
│       │   ├── PasswordStudioScreen.kt, SettingsScreen.kt, ScanningScreen.kt
│       │   ├── LiveThreatScreen.kt, ReportScreen.kt, ThreatIntelScreen.kt
│       │   ├── FamilyProtectionScreen.kt
│       │   └── AttackMatrixScreen.kt                      (MITRE ATT&CK)
│       ├── components/           # Reusable threat cards, status chips, glass surfaces
│       ├── navigation/           # NavHost + bottom navigation (5 tabs, 25 routes)
│       ├── anim/                 # Haptics.kt, Animations.kt, StaggeredEntry
│       └── theme/                # Deep Navy glassmorphism dark theme
├── app/src/main/assets/
│   ├── rakshakx_model/           # On-device ML models
│   │   ├── distilbert/model.onnx + vocab.txt
│   │   ├── indicbert/model.onnx + vocab.txt
│   │   └── model_config.json
│   ├── model-en-us/              # Vosk ASR model (English)
│   └── RXlogo.png
├── ml/                           # Python ML training pipeline
│   ├── train_distilbert.py       # DistilBERT fine-tuning + ONNX export
│   ├── train_indicbert.py        # IndicBERT fine-tuning + ONNX export
│   ├── generate_dataset.py       # Training data generation
│   ├── copy_to_assets.py         # Model → app assets pipeline
│   └── run_all.py                # Master pipeline
├── docs/                         # Developer documentation
│   ├── ARCHITECTURE.md           # System architecture deep-dive
│   ├── SECURITY.md               # Security model, threat model, privacy guarantees
│   ├── ML_PIPELINE.md            # ML training, inference, OTA updates
│   ├── API_REFERENCE.md          # Key public APIs
│   ├── SETUP_GUIDE.md            # Development environment setup
│   ├── CHANGELOG.md              # Version history
│   └── ROADMAP.md                # Feature roadmap
└── .github/workflows/            # CI/CD (build, test, lint, APK tracking)
```

---

## Security & Privacy Model

### Privacy-First Design Principles

1. **Zero data exfiltration** — All ML inference, text analysis, and scoring runs locally on the device. No user data is transmitted to any server.
2. **Encrypted persistence** — All threat data stored in SQLCipher (AES-256) databases with hardware-backed key management via Android Keystore.
3. **Minimal retention** — The app processes messages to detect threats but does not retain raw message content beyond what's needed for the threat log. Automated pruning queries remove old data.
4. **User sovereignty** — Every detection channel can be independently enabled/disabled. Threat intelligence sharing is strictly opt-in and uses irreversible SHA-256 hashing.
5. **No MITM** — The VPN layer inspects DNS metadata only and does not install custom certificates or decrypt TLS traffic.

### Threat Model

| Threat | Mitigation |
|--------|-----------|
| Adversarial text evasion | Hybrid ML + rules scoring; rule engine catches keyword patterns even if ML is fooled |
| Model extraction via APK reverse engineering | ProGuard/R8 obfuscation (planned); ONNX models are INT8 quantized (reduced utility for extraction) |
| Notification content side-channel | NotificationListenerService filters by package name; non-SMS/email/messaging notifications are ignored |
| VPN traffic interception | DNS-only inspection; no custom CA; all HTTPS encryption preserved |
| Local database access (rooted device) | SQLCipher AES-256 encryption; key in Android Keystore (hardware-backed on supported devices) |
| Replay attacks on threat intel | SHA-256 hashing of identifiers; timestamp rounding for k-anonymity |

See [docs/SECURITY.md](docs/SECURITY.md) for the complete security architecture.

---

## Permissions & Justification

| Permission | Security Justification | Attack Surface Protected |
|-----------|----------------------|------------------------|
| `RECEIVE_SMS` / `READ_SMS` | SMS threat ingress and inbox polling fallback | Smishing |
| `READ_CALL_LOG` / `READ_PHONE_STATE` | Call state monitoring, caller ID reputation lookup | Vishing |
| `RECORD_AUDIO` | Call audio capture for on-device transcription (Vosk) | Vishing |
| `SYSTEM_ALERT_WINDOW` | Real-time fraud overlay during active calls | Vishing |
| `POST_NOTIFICATIONS` | Threat alert delivery across all channels | All vectors |
| `INTERNET` / `ACCESS_NETWORK_STATE` | VPN DNS relay to upstream resolver | Web threats |
| `CAMERA` | QR code scanning for malicious URL detection | Quishing (QR phishing) |
| `RECEIVE_BOOT_COMPLETED` | Auto-start protection services on device reboot | Persistence |
| `VIBRATE` | Haptic feedback for threat alerts | UX |
| NotificationListenerService | SMS/email/messaging notification interception | Smishing, phishing, messaging scams |
| AccessibilityService | Browser URL bar monitoring for web threat detection | Web threats |
| VpnService | DNS-level network traffic analysis and blocking | Malicious domains |

The app enforces a progressive onboarding flow — permissions are explained individually with security context before each grant. Accessibility Service is optional during onboarding and prompted when the user enables Web Shield.

---

## Getting Started

### Prerequisites

- **Android Studio** Ladybug (2024.3) or newer
- **JDK 21** (bundled with Android Studio)
- **Android SDK** API 36
- **Git LFS** (ONNX model files are stored via LFS)
- **Python 3.8+** (ML training pipeline only)
- **Physical Android device** (Android 8.0+) — recommended for full testing

### Build & Run

```powershell
# Pull model files via Git LFS
git lfs install
git lfs pull

# Build debug APK (includes ONNX model verification)
.\gradlew.bat assembleDebug

# Install on connected device
.\gradlew.bat installDebug

# Run unit tests
.\gradlew.bat testDebugUnitTest

# Full local CI
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

### ML Training Pipeline

```bash
cd ml/
python -m venv .venv && .venv\Scripts\activate
pip install -r requirements.txt
python run_all.py   # dataset → train DistilBERT → train IndicBERT → export to assets
```

See [docs/ML_PIPELINE.md](docs/ML_PIPELINE.md) for detailed training instructions.

### ONNX Model Requirement

The build requires two ONNX model files in `app/src/main/assets/rakshakx_model/`:
- `distilbert/model.onnx` (minimum 1MB)
- `indicbert/model.onnx` (minimum 500KB)

The Gradle task `verifyRakshakOnnxAssets` fails the build if these are missing or undersized. Pull via Git LFS or train with the ML pipeline.

---

## Configuration

### Model Configuration (`assets/rakshakx_model/model_config.json`)

```json
{
  "distilbert_confidence_threshold": 0.75,
  "hinglish_threshold": 0.65,
  "language_detection_threshold": 0.15,
  "indic_languages": ["hi", "ta", "te", "kn", "ml", "mr", "bn", "gu", "pa", "ur", "or"],
  "channels": ["sms", "email", "call", "web"],
  "models": {
    "distilbert": { "path": "distilbert/model.onnx", "vocab": "distilbert/vocab.txt", "max_seq_len": 128 },
    "indicbert":  { "path": "indicbert/model.onnx",  "vocab": "indicbert/vocab.txt",  "max_seq_len": 128 }
  },
  "labels": ["SAFE", "SCAM", "SUSPICIOUS"],
  "version": "1.0.0"
}
```

### Risk Scoring Thresholds

| Final Score | Classification | Response |
|------------|---------------|----------|
| < 0.40 | SAFE | No alert |
| 0.40 -- 0.69 | SUSPICIOUS | Warning notification (ALERTS_SUSPICIOUS channel) |
| >= 0.70 | SCAM | Critical alert (ALERTS_CRITICAL channel), blocking options |

### RiskEngine Contextual Scoring

The rule-based engine applies 8 keyword categories with contextual modifiers:

| Modifier | Effect | Rationale |
|----------|--------|-----------|
| Sender reputation | +8 to +15 for repeat offenders | Persistent threat actors score higher |
| Time-of-day | Late night (11PM-6AM) = 1.2x | Scams disproportionately target late-night recipients |
| Combination amplification | +10 when urgency + credential keywords co-occur | Multi-signal attacks are more likely malicious |
| Banking false-positive reduction | Score reduction for legitimate debit/balance patterns | Reduces false alarms on genuine bank alerts |
| Multilingual keywords | Hindi, Kannada, Tamil, Telugu keyword lists | Regional scam vocabulary coverage |

---

## Testing

```powershell
.\gradlew.bat testDebugUnitTest           # JVM unit tests
.\gradlew.bat lintDebug                   # Static analysis
.\gradlew.bat connectedDebugAndroidTest   # Instrumented tests (device required)
```

### Demo Mode

- `HackathonModeCallMonitorService` — simulated call fraud detection without real calls
- Correlation Screen "Demo Scenario" toggle — visualizes a multi-stage attack timeline
- `DemoScenario.kt` + `DemoAudioUtils.kt` — test audio generation

---

## Documentation

| Document | Description |
|----------|-------------|
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | System architecture, channel design, service lifecycle, module dependencies |
| [SECURITY.md](docs/SECURITY.md) | Security model, threat model, privacy guarantees, encryption, VPN design |
| [ML_PIPELINE.md](docs/ML_PIPELINE.md) | ML training, ONNX export, language detection, hybrid scoring, OTA updates |
| [API_REFERENCE.md](docs/API_REFERENCE.md) | Key public APIs: ScamClassifierRouter, RiskEngine, CorrelationEngine, FraudDao |
| [SETUP_GUIDE.md](docs/SETUP_GUIDE.md) | Development environment setup, build, test, debug |
| [CHANGELOG.md](docs/CHANGELOG.md) | Version history |
| [ROADMAP.md](docs/ROADMAP.md) | Feature roadmap and technical debt tracking |

---

## Codebase Statistics

| Metric | Count |
|--------|-------|
| Kotlin source files | 193 (+23 new security modules) |
| Core packages | 18 (sms, call, email, web, integration, core/{integrity,appsecurity,network,firewall,privacy,breach,vault,forensics,correlation}, data, notifications, ui, onboarding) |
| UI Screens | 25 (10 original + 15 new screens) |
| Navigation tabs | 5 (Home, Shield, Network, Threats, More) |
| Reusable UI Components | PageHeader, GlassCard, ShieldStatusCard, ThreatCard, SecurityScoreGauge + 12 more |
| Room entities | 6 (SmsEvent, CallEvent, EmailEvent, WebEvent, ThreatSession, RiskScore) |
| Activities | 12 |
| Services | 6 (NotificationListener, Accessibility, VPN, FraudMonitoring, CallRecording, Overlay) |
| Broadcast Receivers | 4 (SMS, Call, Boot, NotificationAction) |
| ML Models | 3 (DistilBERT ONNX, IndicBERT ONNX, Vosk ASR) |
| MITRE ATT&CK techniques | 15 mapped (Mobile framework) |
| MITRE ATT&CK tactics | 13 (Initial Access → Resource Development) |
| Tracker signatures | 50+ (analytics, advertising, crash reporting, fingerprinting, social, profiling) |
| Traffic anomaly detectors | 5 (beaconing, DGA, DNS tunneling, cryptomining, exfiltration) |
| Wi-Fi security checks | 4 (encryption, evil-twin, DNS hijack, captive portal) |
| Network port scan targets | 12 ports (21/22/23/80/443/445/1883/5683/5900/3389/8080/8443) |
| Supported languages | 12 (English + 11 Indic) |
| Correlation strategies | 5 |
| Notification channels | 5 |
| SMS ingress paths | 3 (triple-redundancy) |
| Supported email clients | 6+ (Gmail, Outlook, Yahoo, ProtonMail, Samsung, Spark) |
| Supported messaging apps | 5+ (WhatsApp, Telegram, Signal, Google Messages, etc.) |

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for the full contributor guide.

Quick checklist:
1. Fork and create a feature branch: `git checkout -b feat/your-feature`
2. Ensure `assembleDebug`, `testDebugUnitTest`, and `lintDebug` all pass
3. Update Room schema snapshots if modifying entities
4. Do not modify detection logic, correlation engines, or ML pipelines without explicit approval
5. Open a PR with description and screenshots for UI changes

---

## License

This project is proprietary. Contact maintainers for licensing inquiries.
