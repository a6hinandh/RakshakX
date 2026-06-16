# Call Module

**54 files.** The full call fraud detection pipeline — state monitoring, audio capture, real-time transcription, overlay UI, and ML inference.

## Responsibilities

- Detect incoming/outgoing calls via `CallStateMonitor`
- Capture audio and transcribe in real time using `VoskTranscriber`
- Show floating risk overlay during calls via `OverlayBubbleService`
- Run ML fraud intent classification on transcript segments via `FraudInferenceEngine`
- Pre-screen incoming numbers against `ScamCallDatabase` before answer
- Provide quick actions during calls: End Call, Report Number, Mark Safe

## Sub-packages

| Package | Purpose |
|---------|---------|
| `callanalysis/` | `VoskTranscriber`, `CallStateMonitor`, `OverlayBubbleService` |
| `callanalysis/ui/` | `RakshakXActivity` — debug/test UI for call analysis |
| `core/` | `CallOrchestrator`, `FraudInferenceEngine`, call-specific Room DB |
| `services/` | Foreground services: `CallAnalysisService`, `CallRecordingService`, `RiskScanWorker` |
| `services/hackathon/` | `HackathonModeCallMonitorService` — simulated call for demo/testing |

## Key Data Flow

```
OFFHOOK → CallStateMonitor
  → CallRecordingService (audio capture)
  → VoskTranscriber (streaming STT → partial/final text)
  → FraudInferenceEngine (ML classification)
  → OverlayBubbleService (UI update + contextual warnings)
  → FraudDao.insertCall() (persistence)
  → MultiChannelCorrelationEngine.correlateCallEvent() (cross-channel link)
```

## Important Notes

- Single `NotificationListenerService` constraint: call-related notification ingress routes through the shared `RakshakNotificationListenerService`, not a call-specific listener
- `VoskTranscriber` supports both streaming (`recognize(bytes)`) and file-based (`transcribe(path)`) modes
- The overlay requires `SYSTEM_ALERT_WINDOW` permission; granted via onboarding flow
- Call recording requires `RECORD_AUDIO`; recorded audio is processed locally and not stored after transcription
