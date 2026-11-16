# 🚀 Rift Launcher

**Rift** is an ultra-minimalist, high-performance, categorized Android Launcher built with **Kotlin**, **Jetpack Compose**, and **Material 3 Expressive**.

Designed specifically for modern Android flagships with 120Hz refresh rates, RTL Arabic-first interfaces, and zero-compromise privacy.

---

## ✨ Features

- 📁 **Smart Categorized App Drawer (Smart Launcher Style)**: Automatic zero-latency classification of apps into 6 core categories (General, Communication, Internet, Games, Media, Utilities) with an ergonomic bottom floating pill for comfortable one-handed navigation.
- 🔍 **Instant Arabic & Multilingual Fuzzy Search**: Damerau-Levenshtein fuzzy matching with full Arabic normalization (tashkeel, tatweel, letter unification) answering keystrokes in under 16ms.
- 🎨 **Material 3 Expressive Theme**: Dynamic color extraction (Material You), smooth spring-physics transitions (`MotionScheme.expressive()`), and dark/light mode toggle.
- 🔄 **Non-Play Store Update Ecosystem**:
  - Full first-class compatibility with **[Obtainium](https://github.com/ImranR98/Obtainium)** for automatic background updates.
  - Built-in In-App Updater directly querying GitHub Releases without Google Play Services.
- 🛡️ **Privacy & Performance First**: No ads, no trackers, zero network requests outside opt-in update checks, cold launch < 250ms.

---

## 📲 Installation & Updates

### Method 1: Using Obtainium (Recommended)
1. Install [Obtainium](https://github.com/ImranR98/Obtainium/releases) on your Android device.
2. Add this repository URL:
   ```text
   https://github.com/DeloNest/Rift
   ```
3. Obtainium will automatically track releases and install background updates!

### Method 2: Direct APK Download
Download the latest APK from the [Releases](https://github.com/DeloNest/Rift/releases/latest) page.

---

## 🛠️ Tech Stack & Architecture

- **UI**: 100% Jetpack Compose with Material 3 Expressive tokens & spring motion
- **Architecture**: Modular Clean Architecture (22 Gradle modules)
- **DI**: Dagger Hilt
- **Local Storage**: Room & Proto DataStore
- **Async**: Kotlin Coroutines & Reactive Flows
- **Build System**: Gradle 9.3.1 with Android Gradle Plugin (AGP) 9.1.1, Kotlin 2.3.0, KSP 2.3.0
- **Target OS**: Android 8.0+ (minSdk 26, targetSdk 36, compileSdk 37)

---

## 📄 License
Open source under Apache License 2.0.
