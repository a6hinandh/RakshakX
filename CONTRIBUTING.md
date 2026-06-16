# Contributing to RakshakX

Thank you for your interest in contributing to RakshakX! This guide covers everything you need to get started.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Making Changes](#making-changes)
- [Coding Standards](#coding-standards)
- [Commit Messages](#commit-messages)
- [Pull Request Process](#pull-request-process)
- [Reporting Bugs](#reporting-bugs)
- [Requesting Features](#requesting-features)

## Code of Conduct

This project follows our [Code of Conduct](CODE_OF_CONDUCT.md). By participating, you agree to uphold a welcoming, inclusive environment.

## Getting Started

1. **Fork** the repository on GitHub
2. **Clone** your fork locally:
   ```bash
   git clone https://github.com/your-username/RakshakX.git
   cd RakshakX
   ```
3. **Add upstream** remote:
   ```bash
   git remote add upstream https://github.com/a6hinandh/RakshakX.git
   ```
4. **Create a branch** for your work:
   ```bash
   git checkout -b feat/your-feature-name
   ```

## Development Setup

### Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Android Studio | Ladybug (2024.3) or newer | Required |
| JDK | 21 | Bundled with Android Studio or installed separately |
| Android SDK | API 36 | Install via SDK Manager |
| Python | 3.8+ | Only needed for ML training scripts |
| Git LFS | Latest | For ONNX model files |

### Build

```powershell
# Debug build
.\gradlew.bat assembleDebug

# Run unit tests
.\gradlew.bat testDebugUnitTest

# Run lint
.\gradlew.bat lintDebug
```

### ONNX Models

The build requires ONNX model files in `app/src/main/assets/rakshakx_model/`. If you don't have them, run the ML training pipeline:

```bash
cd ml/
pip install -r requirements.txt
python run_all.py
```

## Making Changes

### Branch Naming

Use prefixes to categorize your branch:

| Prefix | Use Case |
|--------|----------|
| `feat/` | New features |
| `fix/` | Bug fixes |
| `refactor/` | Code restructuring without behavior change |
| `docs/` | Documentation only |
| `test/` | Adding or updating tests |
| `chore/` | Build, CI, dependency updates |

### What to Work On

- Check [open issues](https://github.com/a6hinandh/RakshakX/issues) for tasks labeled `good first issue` or `help wanted`
- Refer to [ROADMAP.md](docs/ROADMAP.md) for planned features
- If you want to work on something not listed, open an issue first to discuss

## Coding Standards

### Kotlin

- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use coroutines for all async work (no RxJava, no raw threads)
- Prefer `val` over `var`; prefer immutable collections
- Use `sealed class` / `sealed interface` for state modeling

### Jetpack Compose

- All new UI must use Compose (no new XML layouts)
- Use Material3 components and the app's theme system
- Keep composables small and focused; extract reusable components to `ui/components/`
- Use `remember` and `derivedStateOf` appropriately to minimize recomposition

### Architecture

- **MVVM + layered architecture**: UI -> ViewModel -> Repository -> Data Source
- Channel-specific code stays in its package (`sms/`, `call/`, `email/`, `web/`)
- Shared ML logic goes in `integration/`
- Cross-channel correlation goes in `core/correlation/`

### Database

- Room for all persistence
- SQLCipher encryption for sensitive data
- Always provide migration paths when changing schemas
- Export schema snapshots to `schemas/` directory

### Security

- Never log sensitive user data (message content, phone numbers, email bodies)
- Use parameterized queries (Room handles this, but be careful with raw queries)
- Validate all external input at system boundaries
- Hash any data before sharing (even opt-in threat intelligence uses SHA-256)

## Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

### Types

| Type | Description |
|------|-------------|
| `feat` | New feature |
| `fix` | Bug fix |
| `docs` | Documentation change |
| `refactor` | Code change that neither fixes a bug nor adds a feature |
| `test` | Adding or updating tests |
| `chore` | Build process, CI, dependencies |
| `perf` | Performance improvement |

### Examples

```
feat(sms): add UPI deep-link detection to RiskEngine
fix(call): resolve nullable crash in OverlayBubbleService coroutine launch
docs: add SECURITY.md and vulnerability reporting process
refactor(integration): lazy-load IndicBERT model on first Indic text
```

## Pull Request Process

1. **Ensure your branch is up to date** with `main`:
   ```bash
   git fetch upstream
   git rebase upstream/main
   ```

2. **Verify the build passes** locally:
   ```powershell
   .\gradlew.bat assembleDebug testDebugUnitTest lintDebug
   ```

3. **Open a PR** with:
   - A clear title following commit message conventions
   - Description of what changed and why
   - Screenshots or screen recordings for UI changes
   - List of tested scenarios

4. **CI must pass** before review. The GitHub Actions workflow runs:
   - Debug build
   - Unit tests
   - Lint checks

5. **Code review** — at least one maintainer approval is required

6. **Squash and merge** is the default merge strategy

### PR Checklist

- [ ] Code compiles without errors
- [ ] Unit tests pass
- [ ] Lint passes (or new warnings are justified)
- [ ] Room schema snapshots updated (if entities changed)
- [ ] No sensitive data in logs or comments
- [ ] UI changes tested on multiple screen sizes

## Reporting Bugs

Use the [Bug Report template](.github/ISSUE_TEMPLATE/bug_report.md) when filing issues. Include:

- Device model and Android version
- Steps to reproduce
- Expected vs actual behavior
- Logcat output (if applicable)
- Screenshots or screen recordings

## Requesting Features

Use the [Feature Request template](.github/ISSUE_TEMPLATE/feature_request.md). Describe:

- The problem you're trying to solve
- Your proposed solution
- Alternatives you've considered
- Whether you're willing to implement it

## Questions?

Open a [discussion](https://github.com/a6hinandh/RakshakX/discussions) or reach out to the maintainers. We're happy to help you get started.
