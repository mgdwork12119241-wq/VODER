# Build status

The Android branch is being assembled in stages. The model download layer is now present and uses direct Hugging Face resolve endpoints with resumable `.part` files and app-private storage.

The remaining production step is linking the qwen3-tts.cpp native source/runtime into the Android CMake target and then adding the GitHub Actions APK build.
