#include <jni.h>
#include <android/log.h>

#include "qwen3_tts_c.h"

#include <algorithm>
#include <cmath>
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <fstream>
#include <mutex>
#include <string>

namespace {
constexpr const char* TAG = "VODERVoiceClone";
constexpr const char* TALKER_NAME = "qwen-talker-0.6b-base-Q8_0.gguf";

std::mutex g_mutex;
qwen3_tts_context_t* g_ctx = nullptr;
std::string g_status = "Native engine not initialized";

void set_status(const std::string& value) {
    g_status = value;
    __android_log_print(ANDROID_LOG_INFO, TAG, "%s", g_status.c_str());
}

bool write_wav_16(const char* path, const float* samples, int32_t count, int32_t sample_rate) {
    if (!path || !samples || count <= 0 || sample_rate <= 0) return false;

    std::ofstream out(path, std::ios::binary | std::ios::trunc);
    if (!out) return false;

    const uint16_t channels = 1;
    const uint16_t bits_per_sample = 16;
    const uint32_t byte_rate = static_cast<uint32_t>(sample_rate) * channels * bits_per_sample / 8;
    const uint16_t block_align = channels * bits_per_sample / 8;
    const uint32_t data_size = static_cast<uint32_t>(count * sizeof(int16_t));
    const uint32_t riff_size = 36u + data_size;

    out.write("RIFF", 4);
    out.write(reinterpret_cast<const char*>(&riff_size), 4);
    out.write("WAVE", 4);
    out.write("fmt ", 4);

    const uint32_t fmt_size = 16;
    const uint16_t audio_format = 1;
    out.write(reinterpret_cast<const char*>(&fmt_size), 4);
    out.write(reinterpret_cast<const char*>(&audio_format), 2);
    out.write(reinterpret_cast<const char*>(&channels), 2);
    out.write(reinterpret_cast<const char*>(&sample_rate), 4);
    out.write(reinterpret_cast<const char*>(&byte_rate), 4);
    out.write(reinterpret_cast<const char*>(&block_align), 2);
    out.write(reinterpret_cast<const char*>(&bits_per_sample), 2);
    out.write("data", 4);
    out.write(reinterpret_cast<const char*>(&data_size), 4);

    for (int32_t i = 0; i < count; ++i) {
        const float x = std::clamp(samples[i], -1.0f, 1.0f);
        const int16_t pcm = static_cast<int16_t>(std::lrintf(x * 32767.0f));
        out.write(reinterpret_cast<const char*>(&pcm), sizeof(pcm));
    }

    return static_cast<bool>(out);
}

bool ensure_loaded(const std::string& model_dir) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_ctx) return true;

    g_ctx = qwen3_tts_init();
    if (!g_ctx) {
        set_status("Failed to initialize Qwen3-TTS");
        return false;
    }

    qwen3_tts_set_backend_preference(QWEN3_TTS_BACKEND_CPU);
    qwen3_tts_set_cpu_threads(4);

    if (!qwen3_tts_load_models_with_name(g_ctx, model_dir.c_str(), TALKER_NAME)) {
        char* err = qwen3_tts_get_last_error(g_ctx);
        set_status(err ? std::string("Model load failed: ") + err : "Model load failed");
        if (err) qwen3_tts_free_string(err);
        qwen3_tts_free(g_ctx);
        g_ctx = nullptr;
        return false;
    }

    set_status("Qwen3-TTS loaded locally (CPU)");
    return true;
}
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mgdwork_voder_voiceclone_VoiceCloneEngine_nativeEngineStatus(JNIEnv* env, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    return env->NewStringUTF(g_status.c_str());
}

extern "C" JNIEXPORT jint JNICALL
Java_com_mgdwork_voder_voiceclone_VoiceCloneEngine_nativeGenerate(
    JNIEnv* env,
    jobject,
    jstring modelDir,
    jstring referenceWav,
    jstring text,
    jstring outputWav) {
    if (!modelDir || !referenceWav || !text || !outputWav) return -1;

    const char* c_model_dir = env->GetStringUTFChars(modelDir, nullptr);
    const char* c_reference = env->GetStringUTFChars(referenceWav, nullptr);
    const char* c_text = env->GetStringUTFChars(text, nullptr);
    const char* c_output = env->GetStringUTFChars(outputWav, nullptr);

    if (!c_model_dir || !c_reference || !c_text || !c_output) {
        if (c_model_dir) env->ReleaseStringUTFChars(modelDir, c_model_dir);
        if (c_reference) env->ReleaseStringUTFChars(referenceWav, c_reference);
        if (c_text) env->ReleaseStringUTFChars(text, c_text);
        if (c_output) env->ReleaseStringUTFChars(outputWav, c_output);
        return -2;
    }

    const bool loaded = ensure_loaded(c_model_dir);
    if (!loaded) {
        env->ReleaseStringUTFChars(modelDir, c_model_dir);
        env->ReleaseStringUTFChars(referenceWav, c_reference);
        env->ReleaseStringUTFChars(text, c_text);
        env->ReleaseStringUTFChars(outputWav, c_output);
        return -3;
    }

    qwen3_tts_params_t params{};
    params.max_audio_tokens = 1024;
    params.temperature = 0.7f;
    params.top_p = 1.0f;
    params.top_k = 50;
    params.n_threads = 4;
    params.print_progress = 0;
    params.print_timing = 0;
    params.repetition_penalty = 1.05f;
    params.language_id = -1;
    params.instruction = nullptr;
    params.speaker = nullptr;
    params.vocoder_left_context_sec = 2.0f;

    __android_log_print(ANDROID_LOG_INFO, TAG, "Generating local voice clone");
    qwen3_tts_result_t result = qwen3_tts_synthesize_with_voice(
        g_ctx, c_text, c_reference, params);

    int rc = 0;
    if (!result.success || !result.audio || result.audio_len <= 0) {
        char* err = result.error_msg;
        if (!err && g_ctx) err = qwen3_tts_get_last_error(g_ctx);
        set_status(err ? std::string("Generation failed: ") + err : "Generation failed");
        if (err && err != result.error_msg) qwen3_tts_free_string(err);
        rc = -4;
    } else if (!write_wav_16(c_output, result.audio, result.audio_len, result.sample_rate)) {
        set_status("Generated audio but failed to write WAV");
        rc = -5;
    } else {
        set_status("Generation complete");
    }

    qwen3_tts_free_result(result);
    env->ReleaseStringUTFChars(modelDir, c_model_dir);
    env->ReleaseStringUTFChars(referenceWav, c_reference);
    env->ReleaseStringUTFChars(text, c_text);
    env->ReleaseStringUTFChars(outputWav, c_output);
    return rc;
}
