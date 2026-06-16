# Web Module

**34 files.** VPN-based network traffic inspection, accessibility service browser monitoring, and URL/QR scanning.

## Responsibilities

- Intercept DNS queries via a local TUN VPN interface
- Analyze domains against threat intelligence, TLS certificate validity, and redirect chains
- Correlate DNS-level events with active browser sessions (URL, form fields detected)
- Block or warn on high-risk domains via `ThreatBlockingEngine`
- Support manual URL scanning and QR code scanning via `UrlScanActivity`
- Run optional web-specific AI scoring via `ModelManager`

## Sub-packages

| Package | Purpose |
|---------|---------|
| `services/` | `FraudVpnService` (TUN VPN), `DnsVpnRelay` |
| `analyzers/` | `DomainRiskAnalyzer`, `FraudRiskAnalyzer`, `BrowserNetworkCorrelationEngine`, `ThreatBlockingEngine`, `ScamLanguageAnalyzer`, `ThreatScoringEngine`, `ThreatIntelRepository` |
| `extractors/` | `PacketParser`, `DnsTrafficAnalyzer`, `RedirectChainTracker` |
| `ai/` | `ModelManager`, `AiThreatScorer`, `FraudTextPreprocessor` |
| `ui/` | `UrlScanActivity` |

## VPN Architecture

The VPN operates DNS-only — it does **not** decrypt HTTPS traffic or install a custom CA:

```
Browser DNS request → TUN interface (10.0.0.0/24)
  → DnsVpnRelay reads DNS UDP packet
  → PacketParser + DnsTrafficAnalyzer extract domain
  → DomainRiskAnalyzer assesses threat level
  → DNS forwarded to real resolver via UDP
  → Response passed back through TUN to browser
```

Domain-level analysis catches phishing sites, typosquatting, and malicious subdomains without compromising TLS security.

## Important Notes

- `FraudVpnService` requires Android VPN permission (`startActivityForResult(VpnService.prepare(ctx))`)
- Web channel status in the UI requires **both** VPN running AND accessibility service readiness
- The `BrowserNetworkCorrelationEngine` reads `BrowserSessionCache.latest()` — set by the accessibility service when it detects URL bar changes
- AI models in `ai/` are optional; the service degrades to heuristics if models fail to load
