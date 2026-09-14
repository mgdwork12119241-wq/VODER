package com.mgdwork.voder.voiceclone

import java.io.File
import java.io.RandomAccessFile

object WavUtils {
    fun validatePcmWav(file: File): Boolean {
        if (!file.exists() || file.length() < 44) return false
        RandomAccessFile(file, "r").use { raf ->
            val riff = ByteArray(4); raf.readFully(riff)
            val wave = ByteArray(4); raf.seek(8); raf.readFully(wave)
            return String(riff, Charsets.US_ASCII) == "RIFF" && String(wave, Charsets.US_ASCII) == "WAVE"
        }
    }
}
