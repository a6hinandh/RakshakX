# RakshakX API Reference

Key public APIs for integration, extension, and testing.

---

## ScamClassifierRouter

**Location:** `integration/ScamClassifierRouter.kt`

The central ML classification entry point. Routes text to the appropriate model and produces a hybrid score.

### Constructor

```kotlin
ScamClassifierRouter(context: Context)
```

Loads model config from `assets/rakshakx_model/model_config.json`, initializes DistilBERT eagerly. IndicBERT is lazy-loaded on first Indic text.

### Methods

#### `classify(text: String, channel: String = "generic"): ModelResult`

Classify text for scam/phishing content.

**Parameters:**
- `text` — Message content to analyze. Can include metadata headers (`From:`, `Caller:`, `URL:`) which are stripped before ML analysis.
- `channel` — Source channel identifier: `"sms"`, `"email"`, `"call"`, `"web"`, `"generic"`

**Returns:** `ModelResult`

```kotlin
data class ModelResult(
    val isScam: Boolean,         // true if finalScore >= 0.40
    val confidence: Float,       // Raw ML model confidence (0.0–1.0)
    val label: String,           // "SAFE", "SUSPICIOUS", or "SCAM"
    val modelUsed: String,       // "distilbert", "indicbert", or "none"
    val channel: String,         // Echo of input channel
    val ruleScore: Int = 0,      // RiskEngine score (0–100)
    val finalScore: Float = 0f   // Weighted hybrid score (0.0–1.0)
)
```

#### `release()`

Release ONNX Runtime sessions and free memory. Call when the classifier is no longer needed.

---

## RiskEngine

**Location:** `sms/RiskEngine.kt`

Rule-based contextual scoring engine. Stateless methods (companion object).

### Methods

#### `calculate(text: String): Int`

Basic keyword-based risk score.

**Returns:** Score from 0–100.

#### `calculate(text: String, sender: String, context: Context): Int`

Contextual scoring with sender reputation and time weighting.

**Parameters:**
- `text` — Message content
- `sender` — Sender identifier (phone number, email address)
- `context` — Android context for time-based features

**Returns:** Score from 0–100, adjusted for:
- Sender reputation (repeated flagged senders get +15)
- Time of day (late night 1.2x, evening 1.15x, business hours 0.95x)
- Banking false-positive reduction
- Combination amplification (urgency + credential keywords = +10)

#### `recordFlaggedSender(sender: String)`

Record that a sender was flagged as suspicious. Increments the sender's flag count for reputation tracking.

#### `getSenderFlagCount(sender: String): Int`

Get the number of times a sender has been flagged.

#### `isLegitimateTransactionAlert(text: String): Boolean`

Check if a message matches legitimate banking transaction patterns (debit alerts, balance notifications). Used to reduce false positives.

---

## MultiChannelCorrelationEngine

**Location:** `core/correlation/MultiChannelCorrelationEngine.kt`

Cross-channel threat correlation. Links events from different channels that appear to be part of the same attack.

### Constructor

```kotlin
MultiChannelCorrelationEngine(fraudDao: FraudDao)
```

### Correlation Types

```kotlin
enum class CorrelationType {
    SMS_WEB_URL,          // SMS link → web visit
    EMAIL_WEB_URL,        // Email link → web visit
    CALL_SMS_PHONE,       // Vishing call → OTP SMS
    PHONE_MULTI_CHANNEL,  // Same number across channels
    TEMPORAL_BURST        // Multiple threats in rapid succession
}
```

### Methods

#### `correlateWebEvent(url: String): CorrelationResult?`

Check if a web URL visit correlates with a recent SMS or email containing the same URL/domain.

#### `correlateCallEvent(phoneNumber: String): CorrelationResult?`

Check if an incoming call correlates with recent SMS from the same phone number.

#### `correlateSmsEvent(sender: String, body: String): CorrelationResult?`

Check if an incoming SMS correlates with recent calls or web events.

#### `correlateWebWithRecentEmail(url: String): CorrelationResult?`

Specific check for email-to-web URL correlation.

### Return Type

```kotlin
data class CorrelationResult(
    val isCorrelated: Boolean,
    val sourceSms: SmsEventEntity?,
    val sourceCall: CallEventEntity?,
    val sourceEmail: EmailEventEntity?,
    val type: CorrelationType?,
    val confidence: Float
)
```

### Time Windows

| Correlation | Window |
|-------------|--------|
| SMS ↔ Web | 1 hour |
| Email ↔ Web | 1 hour |
| Call ↔ SMS | 15 minutes |
| Multi-channel phone | 24 hours |
| Temporal burst | 15 minutes |

---

## ThreatIntelligenceManager

**Location:** `core/threatintel/ThreatIntelligenceManager.kt`

Opt-in anonymous threat intelligence sharing with differential privacy (SHA-256 hashing).

### Singleton Access

```kotlin
ThreatIntelligenceManager.getInstance()
```

### Methods

#### `isOptedIn(): Boolean`

Check if the user has opted into threat intelligence sharing.

#### `setOptIn(enabled: Boolean)`

Enable or disable threat intelligence sharing.

#### `reportThreat(identifier: String, category: String)`

Report a threat indicator (phone number or domain). The identifier is SHA-256 hashed before any sharing.

#### `isKnownThreat(identifier: String): Boolean`

Check if an identifier matches the local blocklist (community-sourced threat data).

#### `getStats(): ThreatIntelStats`

Get statistics: total reports, community blocklist size, last sync time.

---

## ScamCallDatabase

**Location:** `core/callerid/ScamCallDatabase.kt`

Local scam call database for pre-call screening and caller ID.

### Singleton Access

```kotlin
ScamCallDatabase.getInstance()
```

### Methods

#### `getRiskLevel(phoneNumber: String): RiskLevel`

Check a phone number against the database.

**Returns:** `RiskLevel.SAFE`, `RiskLevel.UNKNOWN`, `RiskLevel.SUSPICIOUS`, or `RiskLevel.DANGEROUS`

#### `reportNumber(phoneNumber: String, reason: String)`

Add a number to the reported scam list.

#### `markSafe(phoneNumber: String)`

Mark a previously flagged number as safe (false positive correction).

#### `shouldAutoSilence(phoneNumber: String): Boolean`

Check if an incoming call should be auto-silenced based on the number's risk level and user settings.

---

## FamilyProtectionManager

**Location:** `core/family/FamilyProtectionManager.kt`

Family protection mode with role-based access and simplified UI.

### Roles

```kotlin
enum class FamilyRole {
    ADMIN,   // Can manage members and settings
    ELDER,   // Simplified UI, receives extra guidance
    CHILD,   // Restricted, alerts forwarded to admin
    SELF     // Standard single-user mode
}
```

### Methods

#### `getCurrentRole(): FamilyRole`

Get the current user's role.

#### `setRole(role: FamilyRole)`

Set the current user's role. Affects UI presentation and alert routing.

#### `isSimplifiedUiEnabled(): Boolean`

Check if simplified UI mode is active (typically for Elder role).

#### `addFamilyMember(name: String, role: FamilyRole)`

Add a family member to the protection group.

#### `getFamilyMembers(): List<FamilyMember>`

List all members in the family protection group.

---

## ModelUpdateManager

**Location:** `core/modelupdate/ModelUpdateManager.kt`

OTA model update framework.

### Methods

#### `checkForUpdates(): UpdateResult`

Check if newer model versions are available.

#### `applyUpdate(modelId: String, version: String)`

Download and apply a model update.

#### `rollback(modelId: String)`

Rollback to the previous model version.

#### `getInstalledVersion(modelId: String): String`

Get the currently installed version of a model.

#### `schedulePeriodicChecks(context: Context)`

Schedule WorkManager to check for updates every 12 hours.

---

## FraudDao

**Location:** `data/dao/FraudDao.kt`

Room DAO for all threat data persistence.

### Key Queries

```kotlin
// SMS
fun getAllSmsList(limit: Int): List<SmsEventEntity>
fun findRecentSmsByPhone(phone: String, since: Long): List<SmsEventEntity>
fun findSmsInTimeRange(start: Long, end: Long): List<SmsEventEntity>

// Calls
fun getAllCallsList(): List<CallEventEntity>
fun findRecentCallsByPhone(phone: String, since: Long): List<CallEventEntity>
fun findCallsInTimeRange(start: Long, end: Long): List<CallEventEntity>

// Email
fun getAllEmailsList(): List<EmailEventEntity>
fun findEmailsInTimeRange(start: Long, end: Long): List<EmailEventEntity>

// Correlated sessions
fun getRecentSessionsList(limit: Int): List<ThreatSessionEntity>

// Insert operations
suspend fun insertSms(event: SmsEventEntity): Long
suspend fun insertCall(event: CallEventEntity): Long
```

---

## SecurityDigestWorker

**Location:** `notifications/SecurityDigestWorker.kt`

Daily security summary notification via WorkManager.

### Scheduling

```kotlin
SecurityDigestWorker.scheduleDaily(context)  // Enqueue daily periodic work
SecurityDigestWorker.cancel(context)          // Cancel scheduled work
```

### Behavior

- Runs once daily with a 1-hour initial delay
- Respects battery-not-low constraint
- Queries last 24 hours of threat data
- Generates grouped summary: suspicious SMS count, correlated threats, critical count
- Shows "All Clear" if no threats detected

---

## PageHeader

**Location:** `ui/components/PageHeader.kt`

Shared page header composable used across all 25 screens.

### Signature

```kotlin
PageHeader(title, infoText, onBack?, modifier?, trailing?)
```

Renders the RakshakX logo, screen title, info dialog button, and optional back navigation / trailing elements. Provides a consistent header experience across all security domain screens.

---

## SecurityWidgetProvider

**Location:** `widget/SecurityWidgetProvider.kt`

Home screen AppWidget showing security score and active channels.

### Widget Specs

- Minimum size: 3x2 cells
- Update interval: 30 minutes
- Shows: Security score, protection status, active channel count
- Background: Dark semi-transparent with rounded corners
