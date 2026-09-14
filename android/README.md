# VODER Voice Clone for Android

Experimental Android-native branch of VODER focused only on local voice cloning.

## Target
- Android 8+ / arm64-v8a
- Kotlin UI
- C++17 + Android NDK + CMake native inference
- Qwen3-TTS 0.6B Base, quantized for mobile
- Reference WAV -> cloned speech WAV
- No WebView and no server-side inference

## Model strategy
The model is intentionally NOT committed to Git. The APK will download the selected Qwen3-TTS mobile model on first run and store it in app-private storage. This keeps the repository and APK manageable.

The first implementation targets Qwen3-TTS 0.6B Base with an INT8/Q8 mobile path. If memory or speed is too high on lower-end devices, a Q4 fallback can be added.

## Current status
The Android shell, Compose UI, NDK/CMake target, and JNI bridge are in place. Native Qwen3-TTS inference, model downloader, file picker, audio playback, and progress/error handling are the next implementation stages.

Reference implementation used for the native direction:
https://github.com/predict-woo/qwen3-tts.cpp
