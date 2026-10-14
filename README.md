# 🛑 BasKarBhai (बस कर भाई) — Anti-Doomscroll Meme Interceptor
> **Crafted with 🔥 and Precision by `mdx`**
>
> *Reclaiming human focus from the algorithmic abyss, one savage meme at a time.*

[![Build BasKarBhai Debug APK](https://github.com/maaz/bas_kar_bhai/actions/workflows/build-apk.yml/badge.svg)](https://github.com/maaz/bas_kar_bhai/actions/workflows/build-apk.yml)
![Target SDK](https://img.shields.io/badge/Target%20SDK-34%20(Android%2014)-FF5722?style=flat-square)
![Min SDK](https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0)-00E5FF?style=flat-square)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.23-7C4DFF?style=flat-square)
![AGP](https://img.shields.io/badge/AGP-8.4.0-00E676?style=flat-square)
![Overlay](https://img.shields.io/badge/Permission-Zero--Permission%20Overlay-FF9100?style=flat-square)

---

## ⚡ Overview
**BasKarBhai** is a production-ready, dark-mode Android utility engineered to break addictive dopamine loops on **Instagram Reels** (`com.instagram.android`) and **YouTube Shorts** (`com.google.android.youtube`).

Unlike typical screen-time apps that beg for invasive root or `SYSTEM_ALERT_WINDOW` permissions, BasKarBhai utilizes a high-efficiency **`AccessibilityService` (`RoastAccessibilityService`)** rendering **`TYPE_ACCESSIBILITY_OVERLAY`** windows directly. It combines an intelligent vertical-pager inspection filter with Audio Focus hijacking and escalating meme roasts.

---

## 🏗️ Core Architecture & Engineering

### 1. Zero-Permission Overlay (`RoastAccessibilityService.kt`)
- Runs within the Android Accessibility framework.
- By utilizing `WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY`, overlays render atop other applications without needing explicit `ACTION_MANAGE_OVERLAY_PERMISSION` grants.
- Registers for `TYPE_VIEW_SCROLLED` and `TYPE_WINDOW_STATE_CHANGED` specifically filtered for Instagram and YouTube.

### 2. Smart Reels/Shorts Filter (`FilterUtils.kt`)
- **Instagram DM Immunity**: Recursively inspects the view hierarchy. If conversation indicators (`direct_thread`, `message_composer`, `direct_chat`, etc.) are detected, scroll events are **discarded**.
- **Vertical Pager Validation**: Only increments `sessionReelCount` if full-screen vertical container nodes (`androidx.viewpager.widget.ViewPager`, `RecyclerView`, or identifiers matching `reel_viewer`, `shorts_player`, `clips_video_container`) are active.
- **Debounce Engine**: A strict **1200ms debounce** window ensures that repeated swipe flurries register as a single Reel.

### 3. Dual Real-Time Tracker
- **Floating HUD Pill (`FloatingHudManager.kt`)**: Renders a draggable HUD pill at top-right displaying:
  $$\text{📜 12 Reels} \quad\vert\quad \text{⏱️ 04:15}$$
- **Active Session Timer**: Increments `activeSeconds` every 1000ms via `Handler(Looper.getMainLooper())` while target apps are foregrounded.
- **10-Minute Session Timeout**: If the user leaves Instagram or YouTube for more than 10 minutes (`SESSION_TIMEOUT_MS = 600,000L`), session counters automatically reset.
- **Daily Statistics Engine**: Tracks Daily Reels, Total Time Wasted, and Roasts Dodged with automatic daily date rollover in `SharedPreferences`.

### 4. Audio Focus Hijacking & Meme Escalation Modal (`RoastModalManager.kt`)
When milestone thresholds are reached:
1. **Audio Focus Hijack**: Requests `AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE` via `AudioFocusHelper.kt`, instantly pausing or muting the background Reel or Short.
2. **Audio Alert**: Triggers an alert buzzer through `ToneGenerator` and `RingtoneManager`.
3. **Escalation Tiers**:
   - 🚨 **Tier 1 (10 Reels OR 5 mins)**: *"Bhai kya kar raha hai tu?"* (Ashneer Grover meme) — 3-second locked button countdown before dismiss is allowed.
   - 🛑 **Tier 2 (25 Reels OR 12 mins)**: *"Bas kar bhai! Padhai likhai me dhyan lagao!"* (IAS Babu meme) — Requires rapidly tapping the dismiss button **5 times**.
   - 🚫 **Tier 3 (40 Reels OR 20 mins)**: *"Ruko Jara Sabar Karo!"* (Hindustani Bhau meme) — Automatically triggers `performGlobalAction(GLOBAL_ACTION_BACK)` to exit the Reels player!
   - 💀 **Tier 4 (60 Reels OR 30 mins)**: *"Khatam! Tata! Bye-Bye!"* (Rahul Gandhi meme) — Executes `performGlobalAction(GLOBAL_ACTION_HOME)` to kick the user straight to the phone home screen **PLUS** sends a webhook snitch alert to your friends!

### 5. Friend-Group Snitch Webhook (`WebhookHelper.kt`)
- Supports **Discord** and **Telegram** Webhooks.
- When Tier 4 is hit, an asynchronous OkHttp POST sends:
  ```json
  {
    "username": "BasKarBhai [mdx Shield]",
    "content": "🚨 **DOOMSCROLL SNITCH ALERT!**\n**Pappu** reached **Tier 4 Doomscroll** (60 Reels / 30m) on Instagram Reels!\nBasKarBhai kicked them out to the phone home screen! 💀"
  }
  ```

---

## 📱 Android 13+ & 14 Restricted Settings Resolution

On Android 13 and Android 14, Google restricts accessibility services for apps sideloaded outside the Google Play Store (the setting will appear greyed out as *"Restricted setting"*).

BasKarBhai includes an integrated in-app guide dialog (`RestrictedSettingsDialog.kt`):
1. Open **Settings** $\to$ **Apps** $\to$ **BasKarBhai**.
2. Tap the **3-dot menu (⋮)** in the top right corner.
3. Tap **"Allow restricted settings"** and confirm via biometric/PIN.
4. Go back to **Accessibility** $\to$ **BasKarBhai Doomscroll Protector** $\to$ **Turn ON**.

---

## 🚀 Cloud APK Compiler (GitHub Actions)

You do **not** need Android Studio installed locally! GitHub Actions compiles and packages the debug APK on every push.

### Workflow Configuration (`.github/workflows/build-apk.yml`)
- Runner: `ubuntu-latest`
- Java: OpenJDK 17 (`temurin`)
- Android SDK setup via `android-actions/setup-android@v3`
- Gradle: Complete wrapper with **Gradle 8.7**, **AGP 8.4.0**, and **Kotlin 1.9.23**
- Command:
  ```bash
  chmod +x gradlew && ./gradlew assembleDebug --stacktrace --no-daemon
  ```
- Output Artifact: Downloadable `BasKarBhai-APK` containing `app-debug.apk`.

---

## 🛠️ Local Build Instructions

If compiling locally with JDK 17:
```bash
# Clone the repository
git clone https://github.com/maaz/bas_kar_bhai.git
cd bas_kar_bhai

# Ensure gradlew has execute permissions
chmod +x gradlew

# Run unit tests
./gradlew test

# Assemble Debug APK
./gradlew assembleDebug
```
The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🎨 Engraved Signature
Architected and built with 🔥 by **`mdx`**.
Dedicated to everyone fighting for their focus in an era of infinite scroll algorithms.
