# Integration Module

Purpose: Cross-channel integration and shared coordination between Web, Email, Call, and SMS channels.

## Architecture
RakshakX operates as a unified security app with four detection channels:

1. **Web** — VPN-based traffic interception, accessibility service monitoring, domain/URL analysis
2. **Email** — Email notification interception, phishing detection, threat history
3. **Call** — Call state monitoring, audio analysis, fraud intent classification
4. **SMS** — SMS notification interception, inbox polling, multilingual rule-based fraud scoring

## Shared Components
- `ui/theme/` — Shared Material3 theme (Color, Theme, Typography)
- `RakshakXApplication` — Application class initializing call state monitoring
- `MainActivity` — Unified dashboard with sections for all four channels

## Cross-Channel Integration Points
- **Unified NotificationListenerService** (`sms.NotificationService`) — Routes SMS app notifications to BOTH the SMS risk engine and the call orchestrator
- **Unified BootReceiver** (`sms.BootReceiver`) — Restarts services for both SMS polling and call monitoring after device reboot

Last update:
- Date: 2026-05-09
- Summary: Completed four-channel integration. Merged duplicate Android components.
- Owner: Team
