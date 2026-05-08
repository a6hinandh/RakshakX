# RakshakX Email Threat Intelligence Module

## Overview

The Email Module of RakshakX is a privacy-first, on-device phishing and fraud detection engine designed to intercept malicious email activity before user interaction.

This module performs real-time notification interception and multi-signal threat analysis locally on the Android device without transmitting sensitive user data to external cloud servers.

---

## Core Objectives

- Detect phishing attempts
- Identify malicious URLs
- Analyze social engineering intent
- Detect obfuscated scam text
- Identify dangerous attachments
- Perform explainable risk scoring
- Maintain privacy through edge processing

---

## Current Detection Pipeline

Notification
→ Text Extraction
→ Text Normalization
→ Feature Extraction
→ Risk Scoring
→ Threat Classification

---

## Detection Signals

### URL Intelligence
- Suspicious domains
- Multi-link detection
- URL obfuscation patterns

### Intent Analysis
- Banking keywords
- Urgency phrases
- Social engineering indicators

### Obfuscation Detection
- Symbol replacement
- Excessive capitalization
- Repeated punctuation

### Attachment Risk Analysis
- APK detection
- ZIP/RAR detection
- Executable attachment indicators

---

## Risk Classification

The module aggregates multiple weak signals into a weighted fraud score.

Threat levels:
- SAFE
- MEDIUM RISK
- HIGH RISK

---

## Privacy & Edge Intelligence

RakshakX Email Module is designed as an edge-first security system.

Features:
- On-device processing
- No cloud inference
- No external LLM dependency
- Privacy-preserving architecture
- Offline-capable threat analysis

---

## Future Enhancements

- Gmail API integration
- TinyML phishing classifier
- ONNX/TFLite transformer inference
- OCR attachment scanning
- Sender reputation analysis
- Explainable phishing alerts

---

## Tech Stack

- Kotlin
- Android NotificationListenerService
- Regex-based NLP preprocessing
- Edge AI architecture
- TensorFlow Lite (planned)
- ONNX Runtime Mobile (planned)