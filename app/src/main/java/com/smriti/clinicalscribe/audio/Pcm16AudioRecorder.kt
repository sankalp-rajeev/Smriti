package com.smriti.clinicalscribe.audio

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean

data class Pcm16AudioClip(
    val pcmBytes: ByteArray,
    val sampleRateHz: Int,
    val channelCount: Int,
    val durationMillis: Long
)

class Pcm16AudioRecorder {
    private val isRecording = AtomicBoolean(false)
    private val lock = Any()
    private var recorder: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var startedAtMillis: Long = 0L
    private var output = ByteArrayOutputStream()

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start(): Result<Unit> {
        if (!isRecording.compareAndSet(false, true)) {
            return Result.failure(IllegalStateException("Gemma audio recording is already active."))
        }

        return runCatching {
            synchronized(lock) {
                output = ByteArrayOutputStream()
            }
            val minBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE_HZ,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).takeIf { it > 0 } ?: (SAMPLE_RATE_HZ / 2)
            val bufferSize = minBufferSize.coerceAtLeast(SAMPLE_RATE_HZ)
            val audioRecord = createAudioRecord(bufferSize)
            recorder = audioRecord
            startedAtMillis = System.currentTimeMillis()
            audioRecord.startRecording()
            recordingThread = Thread(
                {
                    readUntilStopped(audioRecord, bufferSize)
                },
                "SmritiGemmaPcmRecorder"
            ).also { it.start() }
        }.onFailure {
            cancel()
        }
    }

    fun stop(): Result<Pcm16AudioClip> {
        if (!isRecording.compareAndSet(true, false)) {
            return Result.failure(IllegalStateException("No Gemma audio recording is active."))
        }
        return runCatching {
            val durationMillis = (System.currentTimeMillis() - startedAtMillis)
                .coerceIn(MIN_DURATION_MILLIS, MAX_DURATION_MILLIS)
            runCatching { recorder?.stop() }
            recordingThread?.join(1_000L)
            val bytes = synchronized(lock) { output.toByteArray() }
            if (bytes.isEmpty()) {
                error("No speech audio was captured.")
            }
            Pcm16AudioClip(
                pcmBytes = bytes,
                sampleRateHz = SAMPLE_RATE_HZ,
                channelCount = CHANNEL_COUNT,
                durationMillis = durationMillis
            )
        }.also {
            release()
        }
    }

    fun cancel() {
        isRecording.set(false)
        runCatching { recorder?.stop() }
        release()
        synchronized(lock) {
            output.reset()
        }
    }

    @SuppressLint("MissingPermission")
    private fun createAudioRecord(bufferSize: Int): AudioRecord {
        return AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE_HZ,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
    }

    private fun readUntilStopped(audioRecord: AudioRecord, bufferSize: Int) {
        val buffer = ByteArray(bufferSize)
        while (isRecording.get()) {
            if (System.currentTimeMillis() - startedAtMillis >= MAX_DURATION_MILLIS) {
                break
            }
            val read = audioRecord.read(buffer, 0, buffer.size)
            if (read > 0) {
                synchronized(lock) {
                    output.write(buffer, 0, read)
                }
            }
        }
    }

    private fun release() {
        runCatching { recorder?.release() }
        recorder = null
        recordingThread = null
        startedAtMillis = 0L
    }

    companion object {
        const val SAMPLE_RATE_HZ = 16_000
        const val CHANNEL_COUNT = 1
        const val MAX_DURATION_SECONDS = 30
        const val MAX_DURATION_MILLIS = MAX_DURATION_SECONDS * 1000L
        private const val MIN_DURATION_MILLIS = 1L
    }
}
