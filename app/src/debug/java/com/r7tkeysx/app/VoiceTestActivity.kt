package com.r7tkeysx.app

import android.app.Activity
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import com.r7tkeysx.app.voice.VoiceInput
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

// Thử model giọng nói không cần nói: adb push file .wav (16 kHz, mono, 16-bit) vào files/voicetest/ rồi mở activity này.
// Kết quả + thời gian chạy ghi ra logcat, thẻ "VoiceTest".
class VoiceTestActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val voice = VoiceInput.get(this)
        MainScope().launch {
            val files = File(filesDir, "voicetest").listFiles()?.filter { it.name.endsWith(".wav") }?.sorted().orEmpty()
            Log.i(TAG, "model installed=${voice.isModelInstalled()} files=${files.size}")
            for (f in files) {
                val samples = readWav(f)
                val t0 = SystemClock.elapsedRealtime()
                val text = voice.transcribeForTest(samples)
                val ms = SystemClock.elapsedRealtime() - t0
                Log.i(TAG, "${f.name} (${samples.size / 16} ms audio) -> ${ms} ms: $text")
            }
            Log.i(TAG, "done")
            finish()
        }
    }

    private fun readWav(f: File): FloatArray {
        val bytes = f.readBytes()
        val pcm = ByteBuffer.wrap(bytes, 44, bytes.size - 44).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        return FloatArray(pcm.remaining()) { pcm.get(it) / 32768f }
    }

    private companion object { const val TAG = "VoiceTest" }
}
