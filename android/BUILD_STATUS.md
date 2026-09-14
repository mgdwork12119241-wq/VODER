# Build status

## Implemented
- Native Android arm64-v8a app (Android 12 / API 31+).
- Qwen3-TTS C++17/GGML runtime pinned as a recursive Git submodule.
- JNI bridge for local voice cloning and WAV output.
- Resumable Hugging Face model downloads into app-private storage.
- SHA-256 verification for both model files.
- Q8_0 0.6B talker + Q8_0 tokenizer download path (~1.3 GB total).
- Simple Android UI: download model, choose WAV reference, enter text, generate and play WAV.
- GitHub Actions workflow prepared to build the release APK and ZIP bundle.

## Verification status
The source/runtime/model integration has been wired, but the GitHub Actions run has not yet produced a confirmed APK artifact in this environment. The next verification gate is the first successful Actions build; only after that should the APK/ZIP be treated as tested.
