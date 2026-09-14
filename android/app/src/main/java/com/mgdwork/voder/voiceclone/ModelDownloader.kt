package com.mgdwork.voder.voiceclone

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

object ModelDownloader {
    data class Asset(
        val name: String,
        val url: String,
        val expectedBytes: Long,
        val sha256: String
    )

    // Compatible with the pinned Danmoreng/qwen3-tts.cpp Android runtime.
    // Files are stored only in app-private storage: files/models/qwen3tts.
    val talkerQ8 = Asset(
        "qwen-talker-0.6b-base-Q8_0.gguf",
        "https://huggingface.co/Serveurperso/Qwen3-TTS-GGUF/resolve/main/qwen-talker-0.6b-base-Q8_0.gguf?download=true",
        992_615_488L,
        "d54dbaf10591421fa764ed630d764efa717ae40cd959bd48c66d4eb1af226426"
    )

    val tokenizerQ8 = Asset(
        "qwen-tokenizer-12hz-Q8_0.gguf",
        "https://huggingface.co/Serveurperso/Qwen3-TTS-GGUF/resolve/main/qwen-tokenizer-12hz-Q8_0.gguf?download=true",
        291_150_624L,
        "1883beeed99348fc35e23dd225e9082f93f6f8c109330a33d935baa8acdbfd94"
    )

    suspend fun download(context: Context, asset: Asset, onProgress: (Long, Long) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = modelDir(context).apply { mkdirs() }
        val target = File(dir, asset.name)
        if (target.exists() && target.length() == asset.expectedBytes && sha256(target) == asset.sha256) {
            onProgress(asset.expectedBytes, asset.expectedBytes)
            return@withContext target
        }
        if (target.exists()) target.delete()

        val part = File(dir, asset.name + ".part")
        var existing = part.length()

        while (true) {
            val connection = (URL(asset.url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 30_000
                readTimeout = 60_000
                requestMethod = "GET"
                instanceFollowRedirects = true
                if (existing > 0) setRequestProperty("Range", "bytes=$existing-")
            }
            try {
                connection.connect()
                val code = connection.responseCode
                if (existing > 0 && code != HttpURLConnection.HTTP_PARTIAL) {
                    existing = 0L
                    part.delete()
                    continue
                }
                if (code !in 200..299) error("Download failed: HTTP $code")

                val remaining = connection.contentLengthLong.coerceAtLeast(0L)
                val total = if (code == HttpURLConnection.HTTP_PARTIAL && remaining > 0) existing + remaining else asset.expectedBytes

                connection.inputStream.use { input ->
                    RandomAccessFile(part, "rw").use { output ->
                        output.seek(existing)
                        val buffer = ByteArray(1024 * 1024)
                        var done = existing
                        while (true) {
                            val n = input.read(buffer)
                            if (n < 0) break
                            output.write(buffer, 0, n)
                            done += n
                            onProgress(done, total)
                        }
                    }
                }
                break
            } finally {
                connection.disconnect()
            }
        }

        if (part.length() != asset.expectedBytes) {
            error("Downloaded ${asset.name} has ${part.length()} bytes; expected ${asset.expectedBytes}")
        }
        if (sha256(part) != asset.sha256) {
            part.delete()
            error("SHA-256 verification failed for ${asset.name}")
        }
        if (target.exists()) target.delete()
        if (!part.renameTo(target)) error("Could not finalize model file")
        target
    }

    fun modelDir(context: Context): File = File(context.filesDir, "models/qwen3tts")

    fun allAssets(): List<Asset> = listOf(talkerQ8, tokenizerQ8)

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
