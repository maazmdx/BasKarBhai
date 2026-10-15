# 🛑 BasKarBhai — Anti-Doomscroll Meme Interceptor
> **Crafted with 🔥 and Precision by `maazmdx`**
>
> *Reclaiming human focus from algorithmic doomscrolling, one savage meme at a time.*

[![Build BasKarBhai Debug APK](https://github.com/maazmdx/BasKarBhai/actions/workflows/build-apk.yml/badge.svg)](https://github.com/maazmdx/BasKarBhai/actions/workflows/build-apk.yml)
![Target SDK](https://img.shields.io/badge/Target%20SDK-34%20(Android%2014)-FF5722?style=flat-square)
![Min SDK](https://img.shields.io/badge/Min%20SDK-29%20(Android%2010)-00E5FF?style=flat-square)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.23-7C4DFF?style=flat-square)
![AGP](https://img.shields.io/badge/AGP-8.4.0-00E676?style=flat-square)
![Overlay](https://img.shields.io/badge/Permission-Zero--Permission%20Overlay-FF9100?style=flat-square)
![Contributor](https://img.shields.io/badge/Contributor-maazmdx-FF3D00?style=flat-square)

---

## ⚡ Overview
**BasKarBhai** is a production-ready, dark-mode Android utility engineered by **maazmdx** to break addictive dopamine loops on **Instagram Reels** (`com.instagram.android`) and **YouTube Shorts** (`com.google.android.youtube`).

Usable out of the box from **Android 10 (API 29)** through **Android 14+ (API 34)** without needing local Android Studio.

---

## 🚀 Fast 1-Tap Setup & Instant Presets

### Instant Activation:
1. Install `BasKarBhai` and open the app.
2. Tap **"Turn On Protection"** $\to$ Toggle the service ON in Accessibility Settings. That's it!
   - **Android 10, 11, 12**: 100% instant 1-tap activation. No "restricted settings" barriers!
   - **Android 13, 14+**: Includes an in-app 3-step guide if sideload restrictions apply.

### 1-Tap Escalation Presets:
Select your intervention intensity with a single tap:
- ⚡ **Strict**: 5 Reels / 3 mins
- ⚖️ **Balanced**: 10 Reels / 5 mins
- 🧘 **Relaxed**: 20 Reels / 10 mins

---

## 🏗️ Core Architecture & Engineering

### 1. Zero-Permission Overlay (`RoastAccessibilityService.kt`)
- Runs within the Android Accessibility framework.
- By utilizing `WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY`, overlays render directly over target apps without requiring invasive runtime `SYSTEM_ALERT_WINDOW` permissions.
- Listens for `TYPE_VIEW_SCROLLED` and `TYPE_WINDOW_STATE_CHANGED` specifically targeting Instagram and YouTube.

### 2. Smart Reels/Shorts Filter (`FilterUtils.kt`)
- **Instagram DM Immunity**: Recursively inspects the view hierarchy. If conversation indicators (`direct_thread`, `message_composer`, `direct_chat`, etc.) are detected, scroll events are **ignored**.
- **Vertical Pager Validation**: Only increments `sessionReelCount` if full-screen vertical container nodes (`androidx.viewpager.widget.ViewPager`, `RecyclerView`, or identifiers matching `reel_viewer`, `shorts_player`, `clips_video_container`) are active.
- **Debounce Engine**: A strict **1200ms debounce** window ensures that repeated swipe flurries register as a single Reel.

### 3. Dual Real-Time Tracker
- **Floating HUD Pill (`FloatingHudManager.kt`)**: Renders a draggable HUD pill at top-right displaying:
  $$\text{📜 12 Reels} \quad\vert\quad \text{⏱️ 04:15} \quad\text{• maazmdx}$$
- **Active Session Timer**: Increments `activeSeconds` every 1000ms via `Handler(Looper.getMainLooper())` while target apps are foregrounded.
- **10-Minute Session Timeout**: If the user leaves Instagram or YouTube for more than 10 minutes (`SESSION_TIMEOUT_MS = 600,000L`), session counters automatically reset.
- **Daily Statistics Engine**: Tracks Daily Reels, Total Time Wasted, and Roasts Dodged with automatic daily date rollover in `SharedPreferences`.

### 4. Audio Focus Hijacking & Voice Roast Modal (`RoastModalManager.kt`)
When milestone thresholds are reached:
1. **Audio Focus Hijack**: Requests `AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE` on `STREAM_MUSIC` via `AudioRoastPlayer.kt`, instantly pausing or muting the background Reel or Short.
2. **Alert Tone & Haptics**: Generates high-frequency alert chimes via `AudioTrack` and triggers physical haptic vibration pulses.
3. **TextToSpeech Voice Roast**: Speaks punchy, realistic wake-up roasts aloud via Android's native TTS engine.
4. **Escalation Tiers**:
   - ⚠️ **Tier 1 (10 Reels OR 5 mins)**: *"REALITY CHECK"* — 3-second locked button countdown before dismiss is allowed.
   - 🔥 **Tier 2 (25 Reels OR 12 mins)**: *"DOOMSCROLL DETECTED"* — Requires rapidly tapping the dismiss button **5 times** to prove you are awake.
   - 🚨 **Tier 3 (40 Reels OR 20 mins)**: *"SYSTEM INTERVENTION"* — Automatically triggers `performGlobalAction(GLOBAL_ACTION_BACK)` to exit the Reels player!
   - 💀 **Tier 4 (60 Reels OR 30 mins)**: *"TOTAL LOCKOUT"* — Executes `performGlobalAction(GLOBAL_ACTION_HOME)` to kick the user straight to the phone home screen **PLUS** sends a webhook snitch alert to your friends!

### 5. Friend-Group Snitch Webhook (`WebhookHelper.kt`)
- Supports **Discord** and **Telegram** Webhooks.
- When Tier 4 is hit, an asynchronous OkHttp POST sends:
  ```json
  {
    "username": "BasKarBhai [maazmdx]",
    "content": "🚨 **DOOMSCROLL SNITCH ALERT!**\n**Player1** reached **Tier 4 Doomscroll** (60 Reels / 30m) on Instagram Reels!\nBasKarBhai kicked them out to the phone home screen! 💀\n*Created by maazmdx*"
  }
  ```

---

## 🛠️ Compiling & Downloading the APK

GitHub Actions automatically compiles and packages the APK on every push.

- **Workflow:** `.github/workflows/build-apk.yml`
- **Runner:** `ubuntu-latest` with JDK 17
- **Download Link:** Go to [**Actions Tab**](https://github.com/maazmdx/BasKarBhai/actions) $\to$ Latest Workflow Run $\to$ **Artifacts** $\to$ Download **`BasKarBhai-APK`**.

---

## 👤 Author & Contributor
- **maazmdx** ([GitHub Profile](https://github.com/maazmdx))
