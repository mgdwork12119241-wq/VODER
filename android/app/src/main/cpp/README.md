# Native runtime integration

The Android JNI layer is intentionally kept separate from the VODER Python runtime. The production integration uses `predict-woo/qwen3-tts.cpp` as the C++17/GGML inference backend, built for arm64-v8a with the Android NDK.

The JNI contract is:

`nativeGenerate(modelDir, referenceWav, text, outputWav)`

The model directory contains the Qwen3-TTS talker GGUF and tokenizer/codec GGUF downloaded by the Android app.
