package com.mgdwork.voder.voiceclone

import android.content.Context
import java.io.File

object VoiceCloneEngine {
    init { System.loadLibrary("voder_voice_clone") }

    external fun nativeEngineStatus(): String
    external fun nativeGenerate(modelDir: String, referenceWav: String, text: String, outputWav: String): Int

    fun modelsReady(context: Context): Boolean {
        val dir = ModelDownloader.modelDir(context)
        return File(dir, ModelDownloader.talkerQ8.name).length() >= ModelDownloader.talkerQ8.expectedBytes * 0.95 &&
            File(dir, ModelDownloader.tokenizer.name).length() >= ModelDownloader.tokenizer.expectedBytes * 0.95
    }
}
