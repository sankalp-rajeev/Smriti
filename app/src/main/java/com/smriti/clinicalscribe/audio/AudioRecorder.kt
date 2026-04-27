package com.smriti.clinicalscribe.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.SystemClock
import java.io.File

class AudioRecorder(context: Context) {
    private val voiceNoteDir = File(context.filesDir, "voice_notes").apply { mkdirs() }
    private var recorder: MediaRecorder? = null
    private var activeFile: File? = null
    private var startedAtMillis: Long = 0L

    fun start(): Result<Unit> {
        if (recorder != null) {
            return Result.failure(IllegalStateException("A voice note is already recording."))
        }

        val output = File(voiceNoteDir, "voice_note_${System.currentTimeMillis()}.m4a")
        return runCatching {
            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64_000)
                setAudioSamplingRate(44_100)
                setMaxDuration(MAX_DURATION_MILLIS)
                setOutputFile(output.absolutePath)
                prepare()
                start()
            }
            activeFile = output
            startedAtMillis = SystemClock.elapsedRealtime()
        }.onFailure {
            releaseRecorder()
            output.delete()
        }
    }

    fun stop(): Result<VoiceNoteMetadata> {
        val output = activeFile
            ?: return Result.failure(IllegalStateException("No active voice note recording."))
        val durationSeconds = ((SystemClock.elapsedRealtime() - startedAtMillis) / 1000L)
            .toInt()
            .coerceIn(1, MAX_DURATION_SECONDS)

        return runCatching {
            recorder?.stop()
            VoiceNoteMetadata(
                audioFilePath = output.absolutePath,
                audioDurationSeconds = durationSeconds
            )
        }.onFailure {
            output.delete()
        }.also {
            releaseRecorder()
        }
    }

    fun cancel() {
        activeFile?.delete()
        releaseRecorder()
    }

    private fun releaseRecorder() {
        recorder?.reset()
        recorder?.release()
        recorder = null
        activeFile = null
        startedAtMillis = 0L
    }

    companion object {
        const val MAX_DURATION_SECONDS = 30
        private const val MAX_DURATION_MILLIS = MAX_DURATION_SECONDS * 1000
    }
}
