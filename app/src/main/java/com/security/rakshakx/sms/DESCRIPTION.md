# SMS Module

**7 files.** Triple-redundancy SMS fraud detection with deduplication, sender reputation tracking, and cross-channel correlation.

## Responsibilities

- Detect scam SMS across Google Messages, Samsung Messages, and other messaging apps
- Deduplicate across three independent ingress paths
- Track flagged sender reputation across sessions
- Trigger cross-channel correlation checks after high-risk SMS detection
- Apply contextual risk scoring (time weighting, banking false-positive reduction, UPI patterns)

## Components

| Class | Purpose |
|-------|---------|
| `SmsScamDetector` | Main classification entry point — invokes `ScamClassifierRouter` |
| `SmsReceiver` | `SMS_RECEIVED` broadcast receiver; records flagged senders; triggers `MultiChannelCorrelationEngine.correlateSmsEvent()` after DB insert |
| `SmsPollingWorker` | WorkManager job polling `content://sms/inbox` as fallback ingress path |
| `SmsDeduplicationGuard` | Time-window hash deduplication; prevents the same message processing through multiple paths |
| `RiskEngine` | Contextual rule-based scorer (companion object, stateless). Includes sender reputation, time weighting, combination amplification, UPI/payment and job/investment scam keyword categories |

## Three Ingress Paths

```
1. RakshakNotificationListenerService  (primary — notification interception)
   ↓
2. SmsReceiver                         (secondary — SMS_RECEIVED broadcast)
   ↓                                    restricted to default SMS app on Android 15+
3. SmsPollingWorker                    (tertiary — inbox polling via ContentProvider)
   ↓
SmsDeduplicationGuard (prevents duplicate processing across all three paths)
```

## Scoring

```
SmsScamDetector → ScamClassifierRouter.classify(text, "sms")
  → RiskEngine.calculate(text, sender, context)  [contextual, 40% weight]
  → DistilBERT / IndicBERT inference             [60% weight]
  → finalScore ≥ 0.40 → SUSPICIOUS notification
  → finalScore ≥ 0.70 → SCAM notification (ALERTS_CRITICAL channel)
  → If suspicious → correlateSmsEvent() for cross-channel matching
```

## Notes

- Android allows only one `NotificationListenerService` per app; all notification-based SMS ingress routes through the shared `RakshakNotificationListenerService` in the `notifications/` package
- `RiskEngine` is a companion object (no instance state); sender reputation state is managed via an in-memory `ConcurrentHashMap`
- On Android 15+, `SMS_RECEIVED` broadcast is only delivered to the default SMS app; notification interception is the primary path for non-default-app installations
