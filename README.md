<div align="center">
  <h1>🚀 NovaMesh Messenger</h1>
  <p><em>Connect Freely. Chat Securely.</em></p>
  <p>A privacy-first, end-to-end encrypted Android messenger — the lovechild of <strong>WhatsApp</strong> and <strong>Snapchat</strong>, built on the <strong>Matrix protocol</strong>.</p>
</div>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/Android-14-3DDC84?logo=android&logoColor=white" alt="Android"/>
  <img src="https://img.shields.io/badge/Matrix-1.11-000000?logo=matrix&logoColor=white" alt="Matrix"/>
  <img src="https://img.shields.io/badge/Signal_Protocol-✔-3A76F0?logo=signal&logoColor=white" alt="Signal Protocol"/>
  <img src="https://img.shields.io/badge/license-MIT-green" alt="License MIT"/>
  <img src="https://img.shields.io/badge/API-26%2B-brightgreen" alt="minSdk"/>
</p>

---

## ✨ Features

### 💬 Chats (WhatsApp DNA)
- **End-to-end encrypted** — Signal Protocol (Double Ratchet + X3DH)
- **Rich messages** — Text, image, video, audio, files, GIFs, stickers
- **Delivery receipts** — Sending ✓, delivered ✓✓, read ✓✓ (blue ticks)
- **Reactions & replies** — Emoji reactions, swipe-to-reply
- **Voice messages** — Waveform visualizer, hold-to-record
- **Disappearing messages** — Configurable self-destruct timer (5s → 24h)
- **Group chats** — 256 members, admin controls, mentions
- **Pinned & archived** — Organise conversations
- **Search** — Filter chats & messages

### 📸 Camera & Snaps (Snapchat DNA)
- **CameraX preview** — Tap to capture, long-press to record video
- **Front/rear flip** — Smooth rotation animation
- **Flash** — On / Off / Auto
- **Self-timer** — 3s / 10s
- **AR face filters** — ML Kit face detection + custom overlays (dog, crown, glasses, rainbow, retro)
- **Drawing tool** — Finger paint on snaps with colour picker
- **Text overlay** — Add text on snaps before sending
- **Send to Chat / Add to Story** — Post-capture bottom sheet

### 🔥 Stories (Instagram/WhatsApp-style)
- **24-hour ephemeral stories** with segmented progress bars
- **Viewers & reactions** — See who viewed, react with emoji
- **Privacy controls** — All contacts, close friends, custom, except…
- **Auto-advance** — Tap left/right to navigate, long-press to pause
- **Swipe-to-dismiss** — Vertical swipe gesture
- **Reply to stories** — Direct reply with text or emoji

### 🔐 Security
- **Signal Protocol** — Double Ratchet algorithm for perfect forward secrecy
- **SQLCipher** — Encrypted local database (AES-256)
- **Biometric lock** — Unlock with fingerprint / face unlock
- **App lock** — PIN or biometric, configurable timeout
- **Certificate pinning** — SSL/TLS pinning against MITM
- **FLAG_SECURE** — Screenshot prevention across the app
- **Ghost mode** — Go invisible to specific contacts

### 🌐 Matrix Protocol
- **Federated** — Use any Matrix-compatible server (matrix.org, self-hosted)
- **Sync** — Real-time message sync via Matrix SDK
- **Push notifications** — Firebase Cloud Messaging (FCM)

### 📞 WebRTC Calls
- **Voice & video calls** — Peer-to-peer via WebRTC (Stream SDK)
- **Noise cancellation** — Background noise suppression
- **Blur background** — Video call privacy
- **Call recording** — Record calls locally

### 🎨 UI / UX
- **Material You** — Dynamic colour theming (Android 12+ Monet engine)
- **Dark mode** — Light/dark/system-aware themes
- **Glassmorphism** — Frosted glass UI for camera overlays
- **Animated transitions** — Screen-level slide/fade animations
- **Edge-to-edge** — Full screen with transparent status/nav bars
- **Foldable support** — Window size classes for large screens

### 🔍 Discover
- **Suggestions** — Discover new contacts based on your network
- **Trending channels** — Public channels with subscriber counts
- **Communities** — Join topic-based communities
- **QR code** — Scan to add contacts
- **Nearby** — Discover people nearby (Bluetooth Low Energy)

---

## 🏗 Tech Stack

| Layer | Technology |
|-------|-----------|
| **Language** | Kotlin 1.9.22 (jvmTarget 17) |
| **UI** | Jetpack Compose (BOM 2024.02.00) + Material 3 |
| **Navigation** | Navigation Compose 2.7.7 (string-based routes) |
| **Database** | Room 2.6.1 + SQLCipher 4.5.4 (AES-256 encrypted) |
| **E2EE** | Signal Protocol (libsignal-client) |
| **Messaging** | Matrix SDK 1.6.10 (matrix-android-sdk2) |
| **Calls** | WebRTC via Stream SDK 1.0.3 |
| **Camera** | CameraX 1.3.1 |
| **ML Kit** | Face detection, selfie segmentation, pose detection |
| **Media** | Media3 (ExoPlayer), Coil (image loading) |
| **Networking** | Retrofit 2.9, OkHttp 4.12 |
| **Push** | Firebase Cloud Messaging |
| **DI** | Manual (AppContainer) — no Hilt overhead |
| **Serialization** | Kotlinx Serialization 1.6.2 |
| **Build** | Gradle 8.5, KSP, AGP 8.2.2 |

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio Hedgehog** (2023.1.1+) or IntelliJ IDEA
- **JDK 17** (bundled with Android Studio)
- **Android SDK** 34+
- **Git**

### Clone & Run
```bash
git clone https://github.com/vikramraju229-creator/novamesh-messenger.git
cd novamesh-messenger

# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest
```

### Open in Android Studio
1. `File → Open →` select `novamesh-messenger/`
2. Wait for Gradle sync to complete
3. Select `app` run configuration
4. Click ▶️ Run

> **Note:** Building for the first time will download dependencies (~500MB). Subsequent builds are incremental and faster.

---

## 🏠 Self-Hosting (Matrix Server)

NovaMesh is **federated** — you can use any Matrix server. To self-host your own:

### Docker (Recommended)
```bash
docker run -it --rm \
  -p 8008:8008 \
  -v /opt/matrix-data:/data \
  -e SYNAPSE_SERVER_NAME=matrix.yourdomain.com \
  -e SYNAPSE_REPORT_STATS=no \
  matrixdotorg/synapse:latest generate

docker run -d \
  --name synapse \
  -p 8008:8008 \
  -v /opt/matrix-data:/data \
  matrixdotorg/synapse:latest
```

Then configure your homeserver URL in `AppContainer.kt` → `MatrixRepository`.

---

## 🧪 Testing

```bash
# Unit tests
./gradlew test

# Specific test class
./gradlew test --tests "com.novamesh.domain.usecase.GetChatsUseCaseTest"

# Instrumented tests (requires emulator/device)
./gradlew connectedAndroidTest
```

---

## 🤝 Contributing

1. **Fork** the repo (top-right corner on GitHub)
2. **Create a feature branch**: `git checkout -b feat/amazing-feature`
3. **Commit** your changes: `git commit -m 'feat: add amazing feature'`
4. **Push**: `git push origin feat/amazing-feature`
5. **Open a Pull Request**

### Commit Conventions
We follow [Conventional Commits](https://www.conventionalcommits.org/):
- `feat:` new feature
- `fix:` bug fix
- `refactor:` code change without feature/fix
- `docs:` documentation
- `test:` adding tests
- `chore:` build/config changes

---

## 📄 License

Distributed under the **MIT License**. See [LICENSE](LICENSE) for more information.

```
MIT License

Copyright (c) 2026 NovaMesh

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction...
```

---

<div align="center">
  <sub>Built with ❤️ using Kotlin, Jetpack Compose & the Matrix protocol</sub>
</div>
