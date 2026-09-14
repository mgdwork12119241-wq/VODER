package com.mgdwork.voder.voiceclone

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var text by remember { mutableStateOf("") }
            var referencePath by remember { mutableStateOf<String?>(null) }
            var status by remember { mutableStateOf(VoiceCloneEngine.nativeEngineStatus()) }
            var busy by remember { mutableStateOf(false) }
            var progress by remember { mutableStateOf(0f) }
            var outputPath by remember { mutableStateOf<String?>(null) }

            val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
                if (uri == null) return@rememberLauncherForActivityResult
                lifecycleScope.launch {
                    try {
                        val copied = withContext(Dispatchers.IO) {
                            val file = File(cacheDir, "voice_reference.wav")
                            contentResolver.openInputStream(uri)?.use { input ->
                                file.outputStream().use { output -> input.copyTo(output) }
                            } ?: error("تعذر قراءة الملف")
                            file
                        }
                        if (!WavUtils.validatePcmWav(copied)) error("اختر ملف WAV صالح")
                        referencePath = copied.absolutePath
                        status = "تم اختيار التسجيل المرجعي"
                    } catch (e: Exception) {
                        status = "خطأ: ${e.message}"
                    }
                }
            }

            MaterialTheme {
                Column(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("VODER Voice Clone", style = MaterialTheme.typography.headlineMedium)
                    Text("استنساخ صوت محلي بالكامل على الهاتف")
                    Text("استخدم صوتك أو صوتًا لديك إذن لاستخدامه.")

                    Button(
                        enabled = !busy,
                        onClick = {
                            busy = true
                            progress = 0f
                            status = "بدء تنزيل النموذج..."
                            lifecycleScope.launch {
                                try {
                                    val assets = ModelDownloader.allAssets()
                                    val total = assets.sumOf { it.expectedBytes }.toFloat()
                                    var completed = 0L
                                    for (asset in assets) {
                                        ModelDownloader.download(this@MainActivity, asset) { done, _ ->
                                            progress = ((completed + done) / total).coerceIn(0f, 1f)
                                        }
                                        completed += asset.expectedBytes
                                    }
                                    progress = 1f
                                    status = "تم تنزيل النموذج والتحقق منه محليًا"
                                } catch (e: Exception) {
                                    status = "فشل التنزيل: ${e.message}"
                                } finally {
                                    busy = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("تنزيل النموذج (~1.3 GB)") }

                    if (busy) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())

                    Button(
                        enabled = !busy,
                        onClick = { picker.launch(arrayOf("audio/wav", "audio/x-wav", "audio/*")) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (referencePath == null) "اختر تسجيل الصوت المرجعي" else "تم اختيار التسجيل المرجعي ✓") }

                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("النص الذي تريد نطقه") },
                        minLines = 3
                    )

                    Button(
                        enabled = !busy && referencePath != null && text.isNotBlank() && VoiceCloneEngine.modelsReady(this@MainActivity),
                        onClick = {
                            val reference = referencePath ?: return@Button
                            busy = true
                            status = "جاري تشغيل Qwen3-TTS محليًا..."
                            lifecycleScope.launch {
                                val out = File(filesDir, "generated_voice_clone.wav")
                                val rc = withContext(Dispatchers.Default) {
                                    VoiceCloneEngine.nativeGenerate(
                                        ModelDownloader.modelDir(this@MainActivity).absolutePath,
                                        reference,
                                        text,
                                        out.absolutePath
                                    )
                                }
                                outputPath = if (rc == 0) out.absolutePath else null
                                status = VoiceCloneEngine.nativeEngineStatus()
                                busy = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("استنساخ الصوت وتوليد WAV") }

                    if (outputPath != null) {
                        Button(onClick = {
                            MediaPlayer().apply {
                                setDataSource(outputPath)
                                setOnCompletionListener { it.release() }
                                prepare()
                                start()
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("تشغيل النتيجة") }
                    }

                    Text(status)
                    Text("المحرك: C++ / NDK / GGML — المعالجة على الجهاز")
                }
            }
        }
    }
}
