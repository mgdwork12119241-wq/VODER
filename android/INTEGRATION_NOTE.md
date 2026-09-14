Verified external runtime: `predict-woo/qwen3-tts.cpp` supports pure C++17 Qwen3-TTS inference and reference-audio voice cloning, with GGUF models and GGML CPU fallback. The Android build will target arm64-v8a and keep model weights outside the APK.

Verified model sources: `cstr/qwen3-tts-0.6b-base-GGUF` and `cstr/qwen3-tts-tokenizer-12hz-GGUF`. The talker Q8_0 is about 940 MB and the tokenizer F16 about 342 MB. Q4_K is smaller but has documented quality regression.
