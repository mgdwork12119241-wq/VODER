package com.mgdwork.voder.voiceclone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    private external fun nativeEngineStatus(): String

    companion object {
        init { System.loadLibrary("voder_voice_clone") }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var text by mutableStateOf("")
            MaterialTheme {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("VODER Voice Clone", style = MaterialTheme.typography.headlineMedium)
                    Text("استنساخ صوت محلي على الهاتف")
                    Button(onClick = { /* file picker will be added next */ }) {
                        Text("اختر تسجيل الصوت المرجعي")
                    }
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("النص الذي تريد نطقه") }
                    )
                    Button(
                        onClick = { /* Qwen3-TTS native inference will be connected next */ },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("استنساخ الصوت وتوليد WAV") }
                    Text(nativeEngineStatus())
                }
            }
        }
    }
}
