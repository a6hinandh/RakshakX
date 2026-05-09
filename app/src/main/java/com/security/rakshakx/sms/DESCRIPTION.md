# SMS Module

Purpose: Real-time SMS fraud detection across all messaging apps.

## Components
- **NotificationService** — Unified NotificationListenerService (serves both SMS and Call channels). Intercepts notifications from Google Messages, Samsung Messages, etc. Routes to RiskEngine for fraud scoring and to RakshakOrchestrator for combined risk analysis.
- **RiskEngine** — Rule-based multilingual fraud scorer (English, Hindi, Kannada, Tamil, Telugu). Detects urgency, credential harvesting, bank impersonation, suspicious URLs, prize scams, and government impersonation.
- **SmsMainActivity** — Permission setup UI for SMS monitoring. Guides users through granting SMS permissions and enabling Notification Access.
- **SmsReceiver** — Fallback BroadcastReceiver for SMS_RECEIVED (only works if RakshakX is the default SMS app on Android 15+).
- **SmsPollingWorker** — WorkManager job that polls the SMS content provider (content://sms/inbox) every 15 seconds for direct inbox scanning.
- **NotificationHelper** — Notification builder for fraud alerts.
- **BootReceiver** — Unified boot receiver that restarts both SMS polling and call monitoring after device reboot.

Last update:
- Date: 2026-05-09
- Summary: Integrated SMS channel into unified RakshakX app. Restructured from standalone project layout. Merged duplicate NotificationListenerService and BootReceiver with call channel.
- Owner: Team

Notes:
- Android only allows ONE NotificationListenerService per app — the unified service lives here
- On Android 15, SMS_RECEIVED broadcast is restricted to default SMS app only
- Primary detection is via NotificationListenerService, with SMS polling as backup
