# Local model downloads

The Android app is designed to download Qwen3-TTS GGUF model assets into app-private storage rather than shipping multi-hundred-megabyte model weights inside the APK.

Recommended deployment assets:

- Talker Q8_0: `qwen3-tts-12hz-0.6b-base-q8_0.gguf` (~940 MB)
- Tokenizer/codec F16: `qwen3-tts-tokenizer-12hz.gguf` (~342 MB)

The talker has a Q4_K variant (~508 MB) when storage/memory is more important than fidelity. The model card warns that Q4_K has a significant quality regression compared with Q8_0.

Source repositories:

- https://huggingface.co/cstr/qwen3-tts-0.6b-base-GGUF
- https://huggingface.co/cstr/qwen3-tts-tokenizer-12hz-GGUF

The runtime must verify downloaded file size/hash before marking each asset complete and resume interrupted downloads where possible.
