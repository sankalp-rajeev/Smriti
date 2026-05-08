package com.smriti.clinicalscribe.audio

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class Pcm16WavEncoderTest {
    @Test
    fun wrapsPcm16MonoAudioInWavContainer() {
        val pcm = byteArrayOf(1, 2, 3, 4)
        val wav = Pcm16WavEncoder.encode(
            pcmBytes = pcm,
            sampleRateHz = 16_000,
            channelCount = 1
        )

        assertEquals("RIFF", String(wav.copyOfRange(0, 4), Charsets.US_ASCII))
        assertEquals("WAVE", String(wav.copyOfRange(8, 12), Charsets.US_ASCII))
        assertEquals("fmt ", String(wav.copyOfRange(12, 16), Charsets.US_ASCII))
        assertEquals("data", String(wav.copyOfRange(36, 40), Charsets.US_ASCII))
        assertEquals(44 + pcm.size, wav.size)
        assertArrayEquals(pcm, wav.copyOfRange(44, wav.size))
    }
}
