package com.smriti.clinicalscribe.audio

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object Pcm16WavEncoder {
    fun encode(clip: Pcm16AudioClip): ByteArray {
        return encode(
            pcmBytes = clip.pcmBytes,
            sampleRateHz = clip.sampleRateHz,
            channelCount = clip.channelCount
        )
    }

    fun encode(
        pcmBytes: ByteArray,
        sampleRateHz: Int = Pcm16AudioRecorder.SAMPLE_RATE_HZ,
        channelCount: Int = Pcm16AudioRecorder.CHANNEL_COUNT
    ): ByteArray {
        require(sampleRateHz > 0) { "sampleRateHz must be positive." }
        require(channelCount > 0) { "channelCount must be positive." }
        require(pcmBytes.isNotEmpty()) { "pcmBytes must not be empty." }

        val bitsPerSample = 16
        val byteRate = sampleRateHz * channelCount * bitsPerSample / 8
        val blockAlign = channelCount * bitsPerSample / 8
        val dataSize = pcmBytes.size
        val riffSize = 36 + dataSize

        val header = ByteBuffer.allocate(WAV_HEADER_BYTES)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putAscii("RIFF")
            .putInt(riffSize)
            .putAscii("WAVE")
            .putAscii("fmt ")
            .putInt(16)
            .putShort(1.toShort())
            .putShort(channelCount.toShort())
            .putInt(sampleRateHz)
            .putInt(byteRate)
            .putShort(blockAlign.toShort())
            .putShort(bitsPerSample.toShort())
            .putAscii("data")
            .putInt(dataSize)
            .array()

        return ByteArrayOutputStream(WAV_HEADER_BYTES + dataSize).use { stream ->
            stream.write(header)
            stream.write(pcmBytes)
            stream.toByteArray()
        }
    }

    private fun ByteBuffer.putAscii(value: String): ByteBuffer {
        put(value.toByteArray(Charsets.US_ASCII))
        return this
    }

    private const val WAV_HEADER_BYTES = 44
}
