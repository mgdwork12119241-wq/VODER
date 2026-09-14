## Native engine

Use the MIT-licensed `predict-woo/qwen3-tts.cpp` source as the native inference backend. The repository includes GGML as a submodule and exposes a C++17 full pipeline with reference-audio voice cloning.

Android integration must build the GGML CPU backend for arm64-v8a and expose a small JNI adapter. The adapter should load the local model directory, run reference-audio speaker extraction and synthesis, and write 24 kHz mono WAV output.

Do not ship model weights in Git. The Android app downloads the selected GGUF assets from Hugging Face and verifies completion before enabling generation.
