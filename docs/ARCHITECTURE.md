# RakshakX System Architecture

This document describes the system architecture of RakshakX, an on-device Android cybersecurity platform that provides real-time threat detection and response across five communication attack surfaces.

---

## Design Philosophy

RakshakX follows three architectural principles:

1. **Defense in depth** — Every attack surface has multiple detection layers (ML, rules, behavioral, reputation). No single bypass defeats the system.
2. **Privacy by architecture** — All computation runs on-device. The system is designed so that data exfiltration is architecturally impossible, not just policy-prohibited.
3. **Correlation over isolation** — Individual channel detectors catch obvious threats. The cross-channel correlation engine catches coordinated attacks that single-channel filters miss — the real differentiator.

---

## System Overview

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

---

## Channel Architecture

### SMS Channel (`sms/` — 7 files)

**Threat coverage:** Smishing, OTP interception, UPI fraud, fake banking alerts, prize scams, job/investment fraud.

**Ingress paths (triple-redundancy):**

```
Path 1: RakshakNotificationListenerService  (primary — OS-level notification interception)
Path 2: SmsReceiver                         (secondary — SMS_RECEIVED broadcast)
Path 3: SmsPollingWorker                    (tertiary — content://sms/inbox WorkManager polling)
   │
   └── SmsDeduplicationGuard (time-window hash prevents triple-processing)
```

Triple-redundancy is necessary because Android's SMS delivery guarantees vary by version and default-app status:
- Android 15+: `SMS_RECEIVED` broadcast restricted to default SMS app
- Notification interception works regardless of default-app status
- Content provider polling catches messages that arrive while the service is restarting

**Detection pipeline:**

```
SMS text → SmsScamDetector.analyze()
  → ScamClassifierRouter.classify(text, "sms")
    → Language detection (Unicode block analysis)
    → DistilBERT or IndicBERT ONNX inference      [60% weight]
    → RiskEngine.calculate(text, sender, context)  [40% weight]
  → If finalScore ≥ 0.40: SmsFraudNotifications (severity-grouped)
  → If suspicious: RiskEngine.recordFlaggedSender(sender)
  → FraudDao.insertSms() → encrypted Room database
  → MultiChannelCorrelationEngine.correlateSmsEvent()
```

**RiskEngine contextual scoring (8 categories):**
- Urgency keywords ("immediate", "expire", "suspend")
- Credential harvesting ("password", "OTP", "verify account")
- Banking/financial ("debit", "credit card", "account blocked")
- URL presence (shortened URLs, suspicious TLDs)
- Prize/lottery ("winner", "reward", "claim")
- Government impersonation ("IT dept", "tax refund")
- UPI/payment ("UPI ID", "Google Pay", "collect request")
- Job/investment ("work from home", "daily earning", "guaranteed returns")
- Multilingual keyword sets (Hindi, Kannada, Tamil, Telugu equivalents)

### Email Channel (`email/` — 17 files)

**Threat coverage:** Credential phishing, spear phishing, BEC, malicious attachments, URL obfuscation, homoglyph attacks.

**Ingress:** `RakshakNotificationListenerService` identifies email packages (Gmail, Outlook, Yahoo, ProtonMail, Samsung Email, Spark) and extracts sender, subject, and snippet from notification extras.

**Multi-signal analysis pipeline (`EmailThreatPipeline`):**

```
Email notification → EmailScamDetector
  → ScamClassifierRouter.classify(text, "email")    [ML + rules hybrid]
  → EmailThreatPipeline orchestrates:
      ├── UrlAnalyzer         — suspicious domains, typosquatting, redirect obfuscation
      ├── IntentAnalyzer      — urgency, credential harvesting, financial pressure
      ├── ObfuscationAnalyzer — homoglyphs, zero-width chars, base64, excessive caps
      ├── AttachmentAnalyzer  — .exe, .zip, .scr, .bat, .cmd, .vbs, .ps1, .jar, .msi, .apk
      └── UrlReputationAnalyzer — domain reputation database lookup
  → If score ≥ 0.50: persist to ThreatDatabase
  → If score ≥ 0.70: ALERTS_CRITICAL notification
  → ThreatCorrelationEngine checks URL against prior SMS/call/web events
```

### Call Channel (`call/` — 54 files)

**Threat coverage:** Tech support scams, law enforcement impersonation, banking fraud, credential harvesting over voice, remote access social engineering.

**Architecture (largest module):**

```
Incoming call → CallStateMonitor detects PHONE_STATE = OFFHOOK
  │
  ├── PreActionDecisionEngine
  │     └── ScamCallDatabase lookup → auto-silence / warn / allow
  │
  ├── OverlayBubbleService (SYSTEM_ALERT_WINDOW)
  │     ├── Live transcript display
  │     ├── Risk meter (green → amber → red)
  │     ├── Contextual fraud phrase warnings
  │     └── Quick actions: End Call │ Report Number │ Mark Safe
  │
  ├── CallRecordingService (foreground, microphone)
  │     └── CallAudioRecorder (16kHz mono, 16-bit PCM)
  │
  ├── VoskTranscriber (streaming ASR)
  │     ├── Partial results → real-time transcript update
  │     └── Final results → sentence-level classification
  │
  ├── FraudInferenceEngine + FraudIntentClassifier
  │     └── Intent categories: financial, tech support, prize,
  │         urgent action, credential harvesting, impersonation
  │
  ├── FraudDao.insertCall() → encrypted persistence
  │
  └── MultiChannelCorrelationEngine.correlateCallEvent()
        └── Cross-reference phone number against recent SMS events
```

**Key design decisions:**
- Audio is processed locally and **not stored after transcription** — only the transcript and risk score are persisted
- `VoskTranscriber` supports both streaming (real-time during call) and file-based (post-call analysis) modes
- The overlay requires `SYSTEM_ALERT_WINDOW` — this is granted via the onboarding flow, not silently
- Pre-call screening via `ScamCallDatabase` can auto-silence known scam numbers before the user picks up

### Web Channel (`web/` — 34 files)

**Threat coverage:** Phishing sites, malware distribution domains, typosquatting, redirect chain attacks, DNS-based threats, malicious QR codes.

**Architecture: VPN + DNS relay + accessibility service:**

```
Browser request → FraudVpnService (TUN interface at 10.0.0.1)
  │
  ├── DnsVpnRelay intercepts DNS query
  │     └── PacketParser extracts domain
  │
  ├── DomainRiskAnalyzer
  │     ├── Known malicious domain database
  │     ├── Typosquatting detection (Levenshtein distance to popular domains)
  │     ├── Suspicious TLD analysis
  │     └── Domain age / registrar heuristics
  │
  ├── TlsMetadataExtractor
  │     └── Certificate info, cipher suite analysis (metadata only, no MITM)
  │
  ├── RedirectChainTracker
  │     └── Multiple DNS lookups in sequence → redirect chain detection
  │
  ├── ScamLanguageAnalyzer
  │     └── Page title / URL path heuristics for scam language
  │
  ├── AiThreatScorer
  │     └── On-device ML model for domain-level fraud scoring
  │
  ├── FraudRiskAnalyzer (aggregates all signals)
  │     └── ThreatBlockingEngine: ALLOW / WARN / BLOCK
  │
  ├── BrowserNetworkCorrelationEngine
  │     └── Links web visit to active browser session from AccessibilityService
  │
  └── VpnThreatLogger → EncryptedStorageManager (AES-256-GCM)
```

**VPN design rationale:** DNS-only interception via a virtual TUN interface. The VPN captures DNS queries and relays them to real DNS servers while analyzing the domain. This avoids HTTPS payload inspection (which would require MITM certificate injection and compromise all TLS connections) and focuses on domain-level threat intelligence, which is sufficient for blocking phishing sites, malware domains, and typosquatting attacks.

**Supplementary web protection:**
- `AccessibilityMonitorService` — monitors browser URL bar changes via AccessibilityService events; `BrowserDataExtractor` parses page title and URL from accessibility nodes
- `QrScannerActivity` — CameraX + ML Kit Barcode scanning for QR code URL analysis before navigation
- `UrlScanActivity` — handles `ACTION_PROCESS_TEXT` intent for context-menu URL scanning from any app

### Messaging Channel (WhatsApp/Telegram/Signal)

**Threat coverage:** Forwarded scam chains, UPI collect request fraud, social engineering via messaging apps.

**Ingress:** `RakshakNotificationListenerService` detects messaging app packages:
- `com.whatsapp`, `com.whatsapp.w4b` (WhatsApp)
- `org.telegram.messenger` (Telegram)
- `org.thoughtcrime.securesms` (Signal)
- `com.google.android.apps.messaging` (Google Messages)
- And other messaging apps

**Analysis:**
- ML classification of message text via `ScamClassifierRouter`
- Forwarded message detection (high-forwarding count = elevated risk)
- UPI deep-link detection (`upi://pay?...` patterns)
- UPI collect request scam detection
- Cross-channel correlation with recent SMS/call/email events

---

## Cross-Channel Correlation Engine

`MultiChannelCorrelationEngine` (`core/correlation/`) is the architectural differentiator — it detects coordinated multi-stage attacks.

### Five Correlation Strategies

| Strategy | Kill Chain Pattern | Detection Mechanism | Time Window | Risk Boost |
|----------|-------------------|---------------------|-------------|------------|
| `SMS_WEB_URL` | Smishing → victim visits phishing site | URL domain matching between SMS body and web visit | 1 hour | +0.25 |
| `EMAIL_WEB_URL` | Phishing email → victim visits spoofed portal | URL domain matching between email and web visit | 1 hour | +0.25 |
| `CALL_SMS_PHONE` | Vishing call → OTP-harvesting SMS | Same phone number in call + SMS within tight window | 15 minutes | +0.35 |
| `PHONE_MULTI_CHANNEL` | Persistent threat actor across channels | Same phone number in 3+ SMS or call + SMS events | 24 hours | +0.55 |
| `TEMPORAL_BURST` | Coordinated blitz across channels | 2+ threats from different channels in rapid succession | 15 minutes | +0.15 |

### Correlation Flow

```
New event arrives (any channel)
  → Extract identifiers: URL domains, phone numbers, sender IDs
  → Query FraudDao for recent events in OTHER channels:
      findRecentSmsByPhone(phone, since)
      findRecentCallsByPhone(phone, since)
      findRecentSmsWithUrl(domain, since)
      findEmailsInTimeRange(start, end)
  → Match against all 5 correlation strategies
  → If correlated:
      Create ThreatSessionEntity linking events
      Calculate escalated combinedRiskScore
      Determine recommendedAction (BLOCK / WARN / MONITOR)
      Trigger ALERTS_CRITICAL notification
```

### Data Model

```kotlin
ThreatSessionEntity(
    sessionId: Long,
    linkedSmsId: Long?,
    linkedCallId: Long?,
    linkedEmailId: Long?,
    linkedWebId: Long?,
    overallThreatScore: Float,
    threatCategory: String,
    correlationReason: String,
    createdAt: Long,
    resolved: Boolean,
    recommendedAction: String
)
```

---

## ML Pipeline Architecture

### Inference Pipeline

```
Input text → ScamClassifierRouter.classify(text, channel)
  │
  ├── detectLanguage(text) → Unicode block analysis
  │     ├── ≥15% Indic script or ≥6 Indic chars → "indic"
  │     └── Otherwise → "english"
  │
  ├── Model selection
  │     ├── "english" → DistilBertClassifier (eager-loaded ONNX session)
  │     └── "indic"   → IndicBertClassifier (lazy-loaded on first use)
  │
  ├── Tokenization
  │     ├── DistilBERT: WordPiece tokenizer → [CLS] tokens [SEP] (max 128)
  │     └── IndicBERT: SentencePiece tokenizer → input_ids, attention_mask (max 128)
  │
  ├── ONNX Runtime inference → 3-class softmax [SAFE, SCAM, SUSPICIOUS]
  │     └── mlScamProb = confidence if SCAM/SUSPICIOUS, else (1.0 - confidence)
  │
  ├── RiskEngine.calculate(text, sender, context) → ruleScore (0–100)
  │
  ├── Hybrid score = (mlScamProb × 0.60) + (ruleScore/100 × 0.40)
  │
  └── Fallback: if DistilBERT confidence < threshold AND Indic script detected
        → retry with IndicBERT (catches Hinglish / code-mixed text)
```

### Lazy Model Loading

`ScamClassifierRouter` initializes DistilBERT at construction time (covers majority of inputs). IndicBERT initialization is deferred to `getIndicBert()` — the first call to classify an Indic-language message. This saves ~200MB memory for English-only users.

### OTA Model Updates

`ModelUpdateManager` supports over-the-air model updates:
```
WorkManager periodic check (every 12 hours)
  → Query update endpoint for latest model metadata
  → Compare version with installed model
  → Download, verify integrity (size, version)
  → Replace model in internal storage
  → ScamClassifierRouter picks up new model on next classification
  → Rollback capability if new model underperforms
```

---

## Service Lifecycle

### Foreground Services

| Service | Type | Lifecycle | Purpose |
|---------|------|-----------|---------|
| `FraudMonitoringForegroundService` | `specialUse` | Persistent (always running) | Orchestrates all channel monitoring |
| `FraudVpnService` | `dataSync` | While VPN enabled | DNS-level web protection |
| `CallRecordingService` | `microphone` | During active calls only | Audio capture for Vosk transcription |
| `OverlayBubbleService` | `specialUse` | During active calls only | Floating risk overlay UI |

### System Services

| Service | Type | Purpose |
|---------|------|---------|
| `RakshakNotificationListenerService` | NotificationListenerService | SMS/email/messaging notification interception (20+ app packages) |
| `AccessibilityMonitorService` | AccessibilityService | Browser URL bar monitoring for web threat detection |

### Background Workers (WorkManager)

| Worker | Schedule | Purpose |
|--------|----------|---------|
| `SecurityDigestWorker` | Daily | 24-hour threat summary notification |
| `SmsPollingWorker` | Periodic | Fallback SMS inbox polling |
| `RiskScanWorker` | On-demand | Background risk recalculation |
| `ModelUpdateManager` | Every 12 hours | OTA model version checks |

### Boot Sequence

```
Device boot → BOOT_COMPLETED broadcast
  → BootReceiver → AppStartupCoordinator.reconcile()
    → PermissionManager.getReadinessState()
    → If permissions granted:
        Start FraudMonitoringForegroundService
        Schedule SmsPollingWorker
        Schedule SecurityDigestWorker
        Initialize ScamClassifierRouter singleton
```

---

## Data Architecture

### Primary Database (Room + SQLCipher)

Accessed via `DatabaseFactory.getInstance()` with AES-256 encryption:

| Entity | Table | Key Fields | Purpose |
|--------|-------|-----------|---------|
| `SmsEventEntity` | `sms_events` | sender, messageBody, fraudRiskScore, detectedKeywords, detectedUrls, sourceType | SMS threat records |
| `CallEventEntity` | `call_events` | phoneNumber, transcript, callDuration, fraudRiskScore, detectedIntent | Call threat records |
| `EmailEventEntity` | `email_events` | senderEmail, subject, previewText, fraudRiskScore, phishingIndicators | Email threat records |
| `WebEventEntity` | `web_events` | url, domain, pageTitle, vpnFlagged, accessibilityFlagged, fraudRiskScore | Web threat records |
| `ThreatSessionEntity` | `threat_sessions` | linkedSmsId, linkedCallId, linkedEmailId, linkedWebId, overallThreatScore, correlationReason | Correlated multi-channel sessions |
| `RiskScoreEntity` | `risk_scores` | phoneNumber, riskScore, timestamp | Per-number risk history |

### Secondary Database (Email-specific)

`email/database/ThreatDatabase` — dedicated email threat persistence:

| Entity | Purpose |
|--------|---------|
| `ThreatEntity` | Detailed email threats with URL/intent/obfuscation analysis breakdown |

### Data Flow

```
Event detection → Channel entity → FraudDao.insert*()
  → CorrelationEngine queries related events across channels
  → UI screens observe via Flow<List<Entity>>
  → SecurityDigestWorker aggregates for daily summary
  → ThreatIntelligenceManager hashes for opt-in community sharing
  → Automated pruning: pruneOldSms/Calls/Emails/WebEvents/Sessions()
```

### DAO Query Patterns

`FraudDao` provides correlation-optimized queries:

```kotlin
// Cross-channel lookups (used by MultiChannelCorrelationEngine)
findRecentSmsByPhone(phone: String, since: Long): List<SmsEventEntity>
findRecentCallsByPhone(phone: String, since: Long): List<CallEventEntity>
findRecentSmsWithUrl(url: String, since: Long): List<SmsEventEntity>

// Time-range queries (used for temporal burst detection)
findSmsInTimeRange(start: Long, end: Long): List<SmsEventEntity>
findCallsInTimeRange(start: Long, end: Long): List<CallEventEntity>
findEmailsInTimeRange(start: Long, end: Long): List<EmailEventEntity>

// Threat session management
getActiveThreatSessions(): Flow<List<ThreatSessionEntity>>
getSuspiciousSessions(minScore: Float): Flow<List<ThreatSessionEntity>>
```

---

## Module Dependency Graph

```
ui/ ──→ core/ ──→ data/
 │        │         ↑
 │        ├──→ integration/ (ScamClassifierRouter, DistilBERT, IndicBERT)
 │        │         ↑
 │        ├──→ sms/ ─┘  (SmsScamDetector → ScamClassifierRouter)
 │        ├──→ email/    (EmailScamDetector → ScamClassifierRouter)
 │        ├──→ call/     (CallScamDetector → ScamClassifierRouter)
 │        └──→ web/      (WebScamDetector → ScamClassifierRouter)
 │
 ├──→ notifications/ (RakshakNotificationListenerService → all channel detectors)
 ├──→ permissions/ (PermissionManager → readiness state model)
 ├──→ onboarding/ (progressive permission setup)
 ├──→ startup/ (AppStartupCoordinator → boot orchestration)
 └──→ widget/ (SecurityWidgetProvider → home screen)
```

All channels converge on `ScamClassifierRouter` for ML classification and `FraudDao` for persistence. Cross-channel correlation flows through `core/correlation/MultiChannelCorrelationEngine`, which queries all channel DAOs.

---

## Key Design Decisions

1. **Single NotificationListenerService** — Android limits one per app. All notification-based ingress (SMS, email, messaging) routes through `RakshakNotificationListenerService` with per-package dispatch logic that identifies 20+ app package names.

2. **DNS-only VPN** — Full traffic interception is architecturally unsound (requires MITM CA installation, breaks TLS guarantees, cannot inspect HTTP/2 or QUIC). DNS-level analysis catches domain-based threats without compromising the device's security posture.

3. **Hybrid ML + Rules (60/40)** — Pure ML models are vulnerable to adversarial evasion and struggle with novel scam patterns not in training data. The rule-based RiskEngine provides a safety net: keyword patterns are immediately detectable, sender reputation tracks repeat offenders, and combination amplification catches multi-signal attacks.

4. **Lazy IndicBERT** — Loading both ONNX models at startup doubles memory usage (~350MB combined). Since most users primarily receive English content, IndicBERT loads on first need, saving ~200MB for English-only users.

5. **Triple SMS ingress** — Android's SMS broadcast restrictions vary by version and default-app status. Three independent paths (notification listener, broadcast receiver, content provider polling) with deduplication ensure no messages are missed regardless of Android version or app configuration.

6. **Encrypted everything** — Threat logs contain sensitive metadata (phone numbers, message snippets, URLs). SQLCipher AES-256 encryption with Android Keystore-backed keys ensures that even on a compromised device, threat data is not readable without the Keystore credential.

7. **No cloud dependencies** — All detection runs on-device. This is a security choice, not a cost optimization — a cybersecurity app that sends user communications to a cloud API is a data exfiltration vector itself.
