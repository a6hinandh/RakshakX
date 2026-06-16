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

---

## System Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│                         PRESENTATION LAYER                           │
│  Jetpack Compose (Material3)  │  Glassmorphism Dark Theme            │
│  Navigation Compose           │  Home Screen Widget (AppWidget)      │
│  Haptic Feedback System       │  Animated Onboarding (Lottie)        │
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
│                          ML / AI LAYER                               │
│  DistilBERT (ONNX, English)   │  IndicBERT (ONNX, 11 Indic langs)  │
│  Vosk ASR (call transcription) │  AiThreatScorer (web fraud)        │
│  FraudIntentClassifier          │  RiskEngine (contextual rules)     │
├──────────────────────────────────────────────────────────────────────┤
│                          SECURITY LAYER                              │
│  SQLCipher (AES-256-CBC)       │  Android Keystore (TEE/StrongBox)  │
│  EncryptedFile (AES-256-GCM)   │  SHA-256 differential privacy      │
│  EncryptedSharedPreferences     │  SMS deduplication guard           │
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
│   ├── web/                      # Web channel — 34 files
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
│   │   ├── correlation/          # MultiChannelCorrelationEngine (5 strategies)
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
│       ├── screens/              # 10 screens (Dashboard, Logs, Correlation, Settings, etc.)
│       ├── components/           # Reusable threat cards, status chips, glass surfaces
│       ├── navigation/           # NavHost + bottom navigation
│       ├── anim/                 # Haptics.kt, Animations.kt
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

The app enforces a progressive onboarding flow — permissions are explained individually with security context before each grant.

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
| Kotlin source files | 163 |
| Core packages | 10 (sms, call, email, web, integration, core, data, notifications, ui, onboarding) |
| Room entities | 6 (SmsEvent, CallEvent, EmailEvent, WebEvent, ThreatSession, RiskScore) |
| Activities | 12 |
| Services | 6 (NotificationListener, Accessibility, VPN, FraudMonitoring, CallRecording, Overlay) |
| Broadcast Receivers | 4 (SMS, Call, Boot, NotificationAction) |
| ML Models | 3 (DistilBERT ONNX, IndicBERT ONNX, Vosk ASR) |
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
