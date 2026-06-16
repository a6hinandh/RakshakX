# Integration Module

**6 files.** The only package all four detection channels share. Provides unified ML inference and hybrid scoring.

## Responsibilities

- Route text to the appropriate ML model based on Unicode script detection
- Run ONNX inference via DistilBERT (English) or IndicBERT (11 Indic languages)
- Combine ML confidence with rule-based `RiskEngine` score in a weighted hybrid
- Expose a single `classify(text, channel)` API that all channels call

## Components

| Class | Purpose |
|-------|---------|
| `ScamClassifierRouter` | Central entry point. Language detection → model routing → hybrid score calculation |
| `DistilBertClassifier` | ONNX inference wrapper with WordPiece tokenizer; loaded eagerly at startup |
| `IndicBertClassifier` | ONNX inference wrapper with SentencePiece tokenizer; **lazy-loaded** on first Indic text |
| `ModelResult` | Return type: `isScam`, `confidence`, `label`, `modelUsed`, `ruleScore`, `finalScore` |

## Scoring Formula

```
ruleScore  = RiskEngine.calculate(text, sender, context)   // 0–100
mlResult   = DistilBERT or IndicBERT inference             // 0.0–1.0

mlScamProb = confidence if label in [SCAM, SUSPICIOUS]
             else 1.0 - confidence

finalScore = mlScamProb × 0.60 + ruleScore/100 × 0.40

label:  < 0.40 → SAFE
        0.40–0.69 → SUSPICIOUS
        ≥ 0.70 → SCAM
```

## Language Detection

Zero-dependency Unicode block analysis inside `ScamClassifierRouter.detectLanguage()`:

- Counts characters per script (Devanagari, Tamil, Telugu, Kannada, Malayalam, Bengali, Gujarati, Gurmukhi, Odia, Arabic/Urdu)
- If any script exceeds 15% of total letters OR ≥ 6 characters → route to IndicBERT
- Otherwise → route to DistilBERT
- Low-confidence DistilBERT result with detectable Indic script → fallback to IndicBERT

## Lazy Loading

`ScamClassifierRouter` initializes `DistilBertClassifier` at construction time (English covers the majority of inputs). `IndicBertClassifier` is initialized on the first call to `getIndicBert()` — saving ~200MB for English-only users.

## Assets Required

```
assets/rakshakx_model/
├── distilbert/
│   ├── model.onnx       # ≥ 1MB (build will fail if absent)
│   └── vocab.txt
├── indicbert/
│   ├── model.onnx       # ≥ 500KB (build will fail if absent)
│   └── vocab.txt
└── model_config.json    # Thresholds, labels, indic language list
```

See [ML_PIPELINE.md](../../../../../../docs/ML_PIPELINE.md) for training and export instructions.
