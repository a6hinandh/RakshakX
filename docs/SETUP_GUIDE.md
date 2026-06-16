# RakshakX Development Setup Guide

Complete guide for setting up the development environment, building the app, training ML models, and testing on a device.

---

## Prerequisites

| Tool | Version | Required For |
|------|---------|-------------|
| Android Studio | Ladybug (2024.3) or newer | IDE, SDK management |
| JDK | 21 | Kotlin compilation (bundled with Android Studio) |
| Android SDK | API 36 | Build target |
| Git LFS | Latest | ONNX model files |
| Python | 3.8+ | ML training scripts only |
| Physical Android device | Android 8.0+ (API 26) | Full testing (VPN, call recording, notifications) |

---

## 1. Clone the Repository

```bash
# Install Git LFS first (needed for ONNX model files)
git lfs install

# Clone
git clone https://github.com/a6hinandh/RakshakX.git
cd RakshakX
```

Git LFS automatically pulls the ONNX model files in `app/src/main/assets/rakshakx_model/`.

---

## 2. Android Studio Setup

1. Open Android Studio
2. File > Open > select the `RakshakX` root directory
3. Wait for Gradle sync to complete (first sync downloads ~500MB of dependencies)
4. If prompted, install missing SDK components via SDK Manager

### SDK Configuration

Ensure these are installed via SDK Manager (Tools > SDK Manager):
- Android SDK Platform 36
- Android SDK Build-Tools (latest)
- Android Emulator (optional, but limited for testing — see Testing section)

---

## 3. Build

### Debug Build

```powershell
.\gradlew.bat assembleDebug
```

### Release Build

```powershell
.\gradlew.bat assembleRelease
```

### Build Verification

The build automatically verifies ONNX models exist via `verifyRakshakOnnxAssets`. If you see:

```
Missing distilbert/model.onnx under .../assets/rakshakx_model
```

You need to either:
- Ensure Git LFS pulled the files: `git lfs pull`
- Or train and export models (see ML Pipeline section below)

---

## 4. ML Training Pipeline

### Setup Python Environment

```bash
cd ml/
python -m venv .venv

# Windows
.venv\Scripts\activate

# Linux/macOS
source .venv/bin/activate

pip install -r requirements.txt
```

### Train Models

```bash
# Full pipeline: generate data → train DistilBERT → train IndicBERT → export to assets
python run_all.py

# Or run individual steps:
python generate_dataset.py     # Generate training data
python train_distilbert.py     # Fine-tune DistilBERT (English)
python train_indicbert.py      # Fine-tune IndicBERT (Indic languages)
python copy_to_assets.py       # Export quantized ONNX to app assets
```

### Model Output

After training, models are placed in:
```
app/src/main/assets/rakshakx_model/
├── distilbert/
│   ├── model.onnx        # Quantized DistilBERT (INT8)
│   └── vocab.txt         # WordPiece vocabulary
├── indicbert/
│   ├── model.onnx        # Quantized IndicBERT (INT8)
│   └── vocab.txt         # SentencePiece vocabulary
└── model_config.json     # Model configuration
```

### Model Configuration

Copy `model_config.example.json` to `model_config.json` and adjust thresholds:

```bash
cp app/src/main/assets/rakshakx_model/model_config.example.json \
   app/src/main/assets/rakshakx_model/model_config.json
```

See [ML_PIPELINE.md](ML_PIPELINE.md) for configuration parameter details.

---

## 5. Install on Device

### Via Android Studio

1. Connect your Android device via USB (enable USB debugging in Developer Options)
2. Select your device in the device dropdown
3. Click Run (green play button)

### Via Command Line

```powershell
.\gradlew.bat installDebug
```

### Via ADB

```powershell
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 6. Testing

### Unit Tests

```powershell
.\gradlew.bat testDebugUnitTest
```

Test files are in `app/src/test/java/com/security/rakshakx/`.

### Lint

```powershell
.\gradlew.bat lintDebug
```

### Instrumented Tests

Requires a connected device or emulator:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

### Full Verification

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

### Testing Limitations on Emulator

Many RakshakX features require a physical device:

| Feature | Emulator | Physical Device |
|---------|----------|----------------|
| SMS detection | Limited (can simulate via ADB) | Full |
| Call recording | Not supported | Full |
| VPN service | Works | Full |
| Notification listener | Works | Full |
| Overlay bubble | Works | Full |
| QR scanning | Limited (camera simulation) | Full |
| Home screen widget | Works | Full |

### Demo/Hackathon Mode

For testing without real calls:
- Use `HackathonModeCallMonitorService` for simulated call fraud detection
- Use the "Demo Scenario" toggle on the Correlation Screen for multi-stage attack timeline

---

## 7. Debugging

### Logcat Filters

```
Tag:ScamClassifierRouter    # ML classification pipeline
Tag:RAKSHAK_DEBUG           # General debug output
Tag:RakshakX-VPN            # VPN service
Tag:SecurityDigest          # Daily digest worker
Tag:VoskTranscriber         # Call transcription
```

### Key Debug Points

- **Classification results:** Filter `ScamClassifierRouter` to see hybrid scoring breakdown (rule score, ML confidence, final score, model used)
- **Correlation events:** Filter `MultiChannelCorrelation` for cross-channel link detection
- **VPN traffic:** Filter `RakshakX-VPN` for domain analysis and threat detection

---

## 8. Project Structure Quick Reference

```
RakshakX/
├── app/src/main/java/com/security/rakshakx/
│   ├── call/           # Call channel (54 files)
│   ├── web/            # Web channel (34 files)
│   ├── email/          # Email channel (17 files)
│   ├── sms/            # SMS channel (7 files)
│   ├── integration/    # Shared ML (ScamClassifierRouter, ONNX classifiers)
│   ├── core/           # Settings, correlation engine, threat intel, family, model updates
│   ├── data/           # Entities, DAOs, repository
│   ├── notifications/  # Notification listener, channels, digest worker
│   ├── ui/             # Compose screens, components, theme
│   ├── permissions/    # Permission readiness model
│   ├── onboarding/     # Progressive permission setup
│   ├── startup/        # Boot orchestration
│   └── widget/         # Home screen widget
├── ml/                 # Python ML training pipeline
├── docs/               # Documentation
└── .github/workflows/  # CI/CD
```

See [ARCHITECTURE.md](ARCHITECTURE.md) for detailed system architecture.
