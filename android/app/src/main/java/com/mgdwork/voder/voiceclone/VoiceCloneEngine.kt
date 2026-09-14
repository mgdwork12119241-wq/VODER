package com.mgdwork.voder.voiceclone

import android.content.Context
import java.io.File

object VoiceCloneEngine {
    init { System.loadLibrary("voder_voice_clone") }

    external fun nativeEngineStatus(): String
    external fun nativeGenerate(modelDir: String, referenceWav: String, text: String, outputWav: String): Int

    fun modelsReady(context: Context): Boolean {
        val dir = ModelDownloader.modelDir(context)
        val talker = File(dir, ModelDownloader.talkerQ8.name)
        val tokenizer = File(dir, ModelDownloader.tokenizerQ8.name)
        return talker.length() == ModelDownloader.talkerQ8.expectedBytes &&
            tokenizer.length() == ModelDownloader.tokenizerQ8.expectedBytes
    }
}
