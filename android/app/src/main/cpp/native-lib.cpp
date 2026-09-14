#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_mgdwork_voder_voiceclone_MainActivity_nativeEngineStatus(JNIEnv* env, jobject) {
    const std::string status = "Qwen3-TTS native engine scaffold ready";
    return env->NewStringUTF(status.c_str());
}
