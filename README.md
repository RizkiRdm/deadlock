# Deadlock

**Long-term, tamper-resistant application locking for Android.**

Deadlock helps people eliminate compulsive app use by locking chosen Android applications behind multi-year cryptographic policies with zero software bypasses or early-unlock backdoors.

> **Quick Links:** [Product Overview](#product-overview) · [Download & Install](#download--install) · [For Tech Recruiters & Engineering Leads](#for-tech-recruiters--engineering-leads) · [Developer Architecture & Build Guide](#developer-architecture--build-guide)

---

## Product Overview

Most app blockers fail when willpower dips because a user can open settings, toggle off a schedule, or uninstall the blocker. Deadlock is built around **intentional friction** and **defense-in-depth enforcement**:

- **Guided First-Launch Onboarding**: A focused 3-screen setup introduces how locks work, explains package-level cryptographic binding, and walks directly into enabling the Hard Lock Interceptor.
- **Streamlined Two-Step App Locking**: Search installed apps, pick a multi-year duration (`1 Year`, `2 Years`, or `Custom`), preview the exact unlock date, confirm irreversibility, and lock immediately.
- **Instant Launch Interception**: Attempting to open a locked app triggers an immediate home-screen redirect and full-screen lockout screen displaying the scheduled unlock date and remaining days.
- **Zero Early-Unlock Backdoors**: Locks bind to the canonical Android package ID (`com.example.app`), survive app restarts, and automatically re-enforce if a restricted app is uninstalled and reinstalled.

---

## Download & Install

### Option 1: Install Pre-Built APK (Recommended for General Users)

1. Go to the **[GitHub Releases](../../releases/latest)** page for this repository and download the latest `deadlock-v*.apk` asset.
2. On your Android device, open the downloaded `.apk` file and allow **"Install unknown apps"** when prompted by Android.
3. Launch **Deadlock** and complete the 3-screen onboarding tour.
4. On the final onboarding screen (or via the Shield icon in the top-right corner), tap **Enable Interceptor** and turn on **Deadlock App Interceptor** in Android Accessibility Settings.

> **Maintainer Note:** Replace or tag releases on GitHub (`git tag v1.0.0 && git push origin v1.0.0`) with an attached signed `.apk` so **[GitHub Releases](../../releases/latest)** serves the installer directly.

### Option 2: Enable Full OS-Level Enforcement (Optional ADB Setup)

Standard installation uses the real-time Accessibility Interceptor. If you want OS-level app hiding, uninstall blocking, safe-boot restriction, and system clock locking, provision Deadlock as **Device Owner** using a computer with ADB:

1. Temporarily remove existing Google/device accounts in Android **Settings > Passwords & Accounts** (required by Android before granting Device Owner; you can sign back in immediately afterward).
2. Enable **USB Debugging** in Android Developer Options and connect via USB.
3. Run:

```bash
adb shell dpm set-device-owner com.example/.device.DeadlockAdminReceiver
```

---

## For Tech Recruiters & Engineering Leads

Deadlock demonstrates production-grade Android systems engineering, security modeling, and modern Jetpack Compose UI architecture:

| Area | Engineering Highlight |
| :--- | :--- |
| **Security & Integrity** | Policies are signed with **Android Keystore HMAC-SHA256** (`PolicyIntegrityManager`) to detect database tampering, paired with **Direct-Boot device-protected storage mirroring** (`PersistentVaultManager`) so locks survive data clears and reboots. |
| **OS & System Integration** | Dual-layer enforcement combining `DevicePolicyManager` (package hiding, uninstall blocking, `DISALLOW_APPS_CONTROL`, `DISALLOW_CONFIG_DATE_TIME`, `DISALLOW_SAFE_BOOT`) with an `AccessibilityService` real-time window interceptor and `ACTION_PACKAGE_ADDED` broadcast monitor. |
| **Modern Android UI** | Built 100% in **Jetpack Compose & Material 3** with edge-to-edge system insets, custom `Canvas` precision geometric vector graphics, locally bundled Google Fonts (`Space Grotesk`, `Plus Jakarta Sans`, `JetBrains Mono`), and adaptive width constraints for tablets/foldables. |
| **Architecture & State** | Clean separation across `data`, `device`, `domain`, `security`, and `ui` layers using MVVM (`ViewModel` + `StateFlow`), Room persistence, Jetpack DataStore preferences, and Navigation Compose. |
| **Testing** | Local JVM unit and **Robolectric** test coverage validating policy time calculations, Keystore HMAC signatures, and resource integrity without requiring a physical emulator. |

---

## Developer Architecture & Build Guide

### System Architecture

```text
┌────────────────────────────────────────────────────────────────────────┐
│                      PERSISTENCE & CRYPTOGRAPHY                        │
│  PolicyRepository  ◄──►  Room DB + Direct-Boot Vault  ◄──►  Keystore   │
│  OnboardingPreferences (Jetpack DataStore)                             │
└──────────┬──────────────────────┬───────────────────────┬──────────────┘
           │                      │                       │
           ▼                      ▼                       ▼
┌───────────────────┐ ┌────────────────────┐ ┌───────────────────────────┐
│DashboardViewModel │ │LockCreationViewModel│ │DeadlockAccessibilityService│
│(Policies & Status)│ │(Apps, Search, Lock)│ │& PackageChangeReceiver    │
└─────────┬─────────┘ └─────────┬──────────┘ └────────────┬──────────────┘
          │                     │                         │
          ▼                     ▼                         ▼
┌───────────────────────────────────────────┐ ┌──────────────────────────┐
│             JETPACK COMPOSE UI            │ │   LockBlockerActivity    │
│                                           │ │  (Full-Screen Lockout)   │
│  OnboardingScreen ──► DashboardScreen     │ └──────────────────────────┘
│                           ├──► LockDetail │
│                           ├──► Settings   │
│                           └──► AppSelector│
│                                 └─► Sheet │
└───────────────────────────────────────────┘
```

### Tech Stack

- **Language & Concurrency**: Kotlin, Coroutines, `StateFlow` / `Flow`
- **UI & Design**: Jetpack Compose (BOM `2024.09.00`), Material 3, Navigation Compose, custom `Canvas` graphics
- **Persistence & Preferences**: Room (`2.7.0` with KSP), Jetpack DataStore Preferences (`1.1.7`)
- **System APIs**: `DevicePolicyManager`, `AccessibilityService`, `BroadcastReceiver`, Android Keystore (`HmacSHA256`)
- **Testing**: JUnit 4, Robolectric (`4.16.1`), Kotlin Coroutines Test

### Prerequisites

- Android Studio (Ladybug or newer) or JDK 11+ with Android SDK (`compileSdk 36`, `minSdk 24`)
- Gradle (`gradle` or Android Studio Gradle Sync)

### Build & Run From Source

```bash
# 1. Clone the repository
git clone https://github.com/<your-username>/deadlock.git
cd deadlock

# 2. Build the debug APK
gradle assembleDebug

# 3. Install on a connected device or emulator
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Run Tests

```bash
gradle :app:testDebugUnitTest
```

For deeper technical details on Device Owner provisioning and OS restrictions, see [`docs/device-management.md`](docs/device-management.md) and [`docs/android-capabilities.md`](docs/android-capabilities.md).
