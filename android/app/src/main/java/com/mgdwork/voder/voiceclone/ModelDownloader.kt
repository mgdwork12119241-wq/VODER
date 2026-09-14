package com.mgdwork.voder.voiceclone

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL

object ModelDownloader {
    data class Asset(val name: String, val url: String, val expectedBytes: Long)

    // Hugging Face resolve URLs. Files are stored in app-private files/models/qwen3tts.
    val talkerQ8 = Asset(
        "qwen3-tts-12hz-0.6b-base-q8_0.gguf",
        "https://huggingface.co/cstr/qwen3-tts-0.6b-base-GGUF/resolve/main/qwen3-tts-12hz-0.6b-base-q8_0.gguf?download=true",
        940_000_000L
    )
    val tokenizer = Asset(
        "qwen3-tts-tokenizer-12hz.gguf",
        "https://huggingface.co/cstr/qwen3-tts-tokenizer-12hz-GGUF/resolve/main/qwen3-tts-tokenizer-12hz.gguf?download=true",
        342_000_000L
    )

    suspend fun download(context: Context, asset: Asset, onProgress: (Long, Long) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "models/qwen3tts").apply { mkdirs() }
        val target = File(dir, asset.name)
        val part = File(dir, asset.name + ".part")
        var existing = if (part.exists()) part.length() else 0L

        val connection = (URL(asset.url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 60_000
            requestMethod = "GET"
            if (existing > 0) setRequestProperty("Range", "bytes=$existing-")
        }
        connection.connect()
        val code = connection.responseCode
        if (existing > 0 && code != HttpURLConnection.HTTP_PARTIAL) {
            existing = 0L
            part.delete()
            connection.disconnect()
            return@withContext download(context, asset, onProgress)
        }
        if (code !in 200..299) error("Download failed: HTTP $code")

        val total = if (code == HttpURLConnection.HTTP_PARTIAL) {
            existing + connection.contentLengthLong.coerceAtLeast(0L)
        } else connection.contentLengthLong.coerceAtLeast(asset.expectedBytes)

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
        connection.disconnect()
        if (part.length() < asset.expectedBytes * 0.95) error("Downloaded file is incomplete")
        if (target.exists()) target.delete()
        if (!part.renameTo(target)) error("Could not finalize model file")
        target
    }

    fun modelDir(context: Context): File = File(context.filesDir, "models/qwen3tts")
}
