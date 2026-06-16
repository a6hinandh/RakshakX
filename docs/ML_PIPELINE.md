# RakshakX ML Pipeline

This document covers the machine learning infrastructure: model training, configuration, on-device inference, language detection, OTA updates, and the hybrid scoring system.

---

## Overview

RakshakX uses a hybrid ML + rules approach for threat classification:

```
Input text → Language detection (Unicode heuristics)
  ├─ English/Latin → DistilBERT (ONNX)
  └─ Indic script  → IndicBERT (ONNX, lazy-loaded)
                          ↓
              ML confidence score (0.0 – 1.0)
                          ↓
         Hybrid score = ML × 0.60 + Rules × 0.40
                          ↓
         SAFE (<0.40) | SUSPICIOUS (0.40–0.69) | SCAM (≥0.70)
```

---

## Models

### DistilBERT (English)

- **Architecture:** DistilBERT base uncased, fine-tuned for scam/phishing classification
- **Input:** Tokenized English text (WordPiece, max 128 tokens)
- **Output:** 3-class softmax (SAFE, SCAM, SUSPICIOUS)
- **Runtime:** ONNX Runtime 1.19.2
- **Location:** `assets/rakshakx_model/distilbert/model.onnx`
- **Loading:** Eager (loaded at app startup)

### IndicBERT (11 Indian Languages)

- **Architecture:** ai4bharat/IndicBERT, fine-tuned for scam/phishing in Hindi, Tamil, Telugu, Kannada, Malayalam, Bengali, Gujarati, Punjabi, Marathi, Urdu, Odia
- **Input:** Tokenized Indic text (SentencePiece, max 128 tokens)
- **Output:** 3-class softmax (SAFE, SCAM, SUSPICIOUS)
- **Runtime:** ONNX Runtime 1.19.2
- **Location:** `assets/rakshakx_model/indicbert/model.onnx`
- **Loading:** Lazy — initialized on first Indic-language text detection to save ~200MB memory

### Vosk STT (Call Transcription)

- **Architecture:** Vosk lightweight English model
- **Input:** PCM audio stream (16kHz, 16-bit, mono)
- **Output:** Real-time text transcription with partial/final results
- **Location:** `assets/model-en-us/`
- **Usage:** Call channel only, via `VoskTranscriber`

### Web Fraud Models (Optional)

- **Architecture:** TFLite or ONNX models loaded via `ModelManager`
- **Purpose:** Web-specific fraud scoring (supplements domain heuristics)
- **Loading:** Optional, graceful degradation to heuristics if unavailable

---

## Training Pipeline

### Directory Structure

```
ml/
├── generate_dataset.py    # Generates training data
├── train_distilbert.py    # Fine-tunes DistilBERT
├── train_indicbert.py     # Fine-tunes IndicBERT
├── copy_to_assets.py      # Exports quantized ONNX to app assets
├── run_all.py             # Master pipeline (runs all steps)
└── requirements.txt       # Python dependencies
```

### Running the Full Pipeline

```bash
cd ml/
python -m venv .venv

# Windows
.venv\Scripts\activate

# Linux/macOS
source .venv/bin/activate

pip install -r requirements.txt

# Run everything: dataset → train DistilBERT → train IndicBERT → copy to assets
python run_all.py
```

### Running Individual Steps

```bash
# Step 1: Generate training dataset
python generate_dataset.py

# Step 2: Train DistilBERT (English)
python train_distilbert.py

# Step 3: Train IndicBERT (Indic languages)
python train_indicbert.py

# Step 4: Export quantized ONNX models to app assets
python copy_to_assets.py
```

### Build Verification

The Gradle task `verifyRakshakOnnxAssets` runs before every build and fails if:
- `distilbert/model.onnx` is missing or < 1MB
- `indicbert/model.onnx` is missing or < 500KB

---

## Model Configuration

### File: `assets/rakshakx_model/model_config.json`

```json
{
  "distilbert_confidence_threshold": 0.75,
  "hinglish_threshold": 0.65,
  "language_detection_threshold": 0.15,
  "indic_languages": ["hi", "ta", "te", "kn", "ml", "mr", "bn", "gu", "pa", "ur", "or"],
  "channels": ["sms", "email", "call", "web"],
  "models": {
    "distilbert": {
      "path": "rakshakx_model/distilbert/model.onnx",
      "vocab": "rakshakx_model/distilbert/vocab.txt",
      "max_seq_len": 128
    },
    "indicbert": {
      "path": "rakshakx_model/indicbert/model.onnx",
      "vocab": "rakshakx_model/indicbert/vocab.txt",
      "max_seq_len": 128
    }
  },
  "labels": ["SAFE", "SCAM", "SUSPICIOUS"],
  "version": "1.0.0"
}
```

### Configuration Parameters

| Parameter | Default | Purpose |
|-----------|---------|---------|
| `distilbert_confidence_threshold` | 0.75 | Minimum ML confidence to trust DistilBERT result without fallback |
| `hinglish_threshold` | 0.65 | Threshold for mixed Hindi-English (Hinglish) text routing |
| `language_detection_threshold` | 0.15 | Minimum ratio of Indic script characters to trigger IndicBERT |
| `indic_languages` | 11 codes | ISO 639-1 codes for supported Indic languages |
| `max_seq_len` | 128 | Maximum token sequence length for ONNX inference |
| `labels` | SAFE, SCAM, SUSPICIOUS | Output class labels (must match training labels) |
| `version` | 1.0.0 | Model version for OTA update tracking |

---

## Language Detection

`ScamClassifierRouter.detectLanguage()` uses zero-dependency Unicode block analysis:

### Script Ranges

| Language | Unicode Range | ISO Code |
|----------|--------------|----------|
| Hindi/Marathi | `ऀ–ॿ` (Devanagari) | `hi` |
| Tamil | `஀–௿` | `ta` |
| Telugu | `ఀ–౿` | `te` |
| Kannada | `ಀ–೿` | `kn` |
| Malayalam | `ഀ–ൿ` | `ml` |
| Bengali | `ঀ–৿` | `bn` |
| Gujarati | `઀–૿` | `gu` |
| Punjabi | `਀–੿` (Gurmukhi) | `pa` |
| Odia | `଀–୿` | `or` |
| Urdu | `؀–ۿ` (Arabic) | `ur` |

### Routing Logic

1. Filter input to letters only
2. Count characters in each script range
3. If any script exceeds 15% of total letters OR ≥6 characters → route to IndicBERT
4. Otherwise → route to DistilBERT

### Fallback Behavior

If DistilBERT returns low confidence (< `distilbert_confidence_threshold`), the router re-checks language detection and falls back to IndicBERT if Indic script is detected. This catches Hinglish (mixed Hindi-English) text that has too few Indic characters for initial detection.

---

## Hybrid Scoring (ScamClassifierRouter)

### Score Calculation

```
ruleScore = RiskEngine.calculate(text)           // 0–100
mlResult  = DistilBERT/IndicBERT.classify(text)  // 0.0–1.0

mlScamProb = if (mlResult.label in [SCAM, SUSPICIOUS])
                 mlResult.confidence
             else
                 1.0 - mlResult.confidence

finalScore = (mlScamProb × 0.60) + (ruleScore/100 × 0.40)
```

### Classification Thresholds

| Final Score | Label | Action |
|-------------|-------|--------|
| < 0.40 | SAFE | No alert |
| 0.40 – 0.69 | SUSPICIOUS | Warning notification |
| ≥ 0.70 | SCAM | Critical alert |

### RiskEngine Contextual Scoring

The rule-based `RiskEngine` applies weighted keyword scoring with these enhancements:

- **Sender reputation:** Repeated flagged senders get +15 score boost
- **Time weighting:** Late night (11PM–6AM) = 1.2x, evening = 1.15x, business hours = 0.95x
- **Combination amplification:** Urgency + credential keywords together = +10
- **Banking false-positive reduction:** Legitimate transaction patterns (debit alerts, balance notifications) get score reduction
- **UPI/Payment keywords:** Dedicated category for UPI fraud patterns
- **Job/Investment scam keywords:** Dedicated category

---

## OTA Model Updates

`ModelUpdateManager` supports over-the-air model updates without requiring a full app update:

### Update Flow

```
WorkManager periodic check (every 12 hours)
  → Query update endpoint for latest model metadata
  → Compare version with installed model
  → If newer: download model file
  → Verify integrity (size, version)
  → Replace model in internal storage
  → ScamClassifierRouter picks up new model on next classification
```

### Rollback

If a new model produces worse results (tracked via user feedback and automated metrics), `ModelUpdateManager` supports:
- Version tracking for all installed models
- Rollback to the previous model version
- Bundled models (from APK) always available as ultimate fallback

---

## Performance Considerations

### Memory

| Component | Approximate Memory |
|-----------|-------------------|
| DistilBERT ONNX session | ~150MB |
| IndicBERT ONNX session | ~200MB |
| Vosk STT model | ~50MB |
| RiskEngine (rules) | <1MB |

IndicBERT's lazy loading saves ~200MB for users who only receive English content.

### Inference Latency

| Operation | Target | Notes |
|-----------|--------|-------|
| SMS/email classification | < 200ms | Single text classification |
| Call transcript segment | < 300ms | Partial results from Vosk + ML classification |
| Web domain analysis | < 50ms | Primarily rule-based + database lookup |

### Battery Impact

Target: < 3% daily battery with all channels active. Key optimizations:
- WorkManager respects battery-not-low constraints for digest and model updates
- VPN DNS relay is event-driven (no polling)
- Notification listener is passive (OS-triggered callbacks)
