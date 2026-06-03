package com.moriafly.jaudiotagger

import org.jaudiotagger.audio.SupportedFileFormat
import org.jaudiotagger.audio.ogg.util.OpusIdentificationHeader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for Opus format support.
 */
class OpusTest {

    @Test
    fun `opus supported file format exists`() {
        val format = SupportedFileFormat.valueOf("OPUS")
        assertEquals("opus", format.filesuffix)
        assertEquals("Opus", format.displayName)
    }

    /**
     * Test OpusHead header parsing — the raw page data starts directly with "OpusHead",
     * NO separate packet type byte (unlike Vorbis).
     *
     * Per RFC 7845, a standard stereo OpusHead is 19 bytes:
     *   "OpusHead"(8) + version(1) + channels(1) + pre-skip(2)
     *   + input_sample_rate(4) + output_gain(2) + mapping_family(1)
     */
    @Test
    fun `parse valid OpusHead header with stereo`() {
        val data = byteArrayOf(
            'O'.code.toByte(), 'p'.code.toByte(), 'u'.code.toByte(), 's'.code.toByte(),
            'H'.code.toByte(), 'e'.code.toByte(), 'a'.code.toByte(), 'd'.code.toByte(),
            1,  // version = 1
            2,  // channels = 2
            0x38, 0x01,  // pre-skip = 312 (0x0138) LE
            (0x80).toByte(), (0xBB).toByte(), 0x00, 0x00,  // input sample rate = 48000 LE
            0x00, 0x00,  // output gain = 0 (Q7.8)
            0   // channel mapping family = 0
        )

        val header = OpusIdentificationHeader(data)

        assertTrue(header.isValid)
        assertEquals("Opus", header.encodingType)
        assertEquals(2, header.channelNumber)
        assertEquals(312, header.preSkip)
        assertEquals(48000, header.inputSampleRate)
        assertEquals(48000, header.samplingRate) // Always 48000
        assertEquals(0, header.outputGain)
        assertEquals(0.0, header.outputGainDB)
        assertEquals(0, header.channelMappingFamily)
        assertEquals(1, header.streamCount) // Stereo = 1 stream
        assertEquals(1, header.coupledCount) // Stereo = 1 coupled
        assertEquals(1, header.version)
    }

    @Test
    fun `parse OpusHead mono`() {
        val data = byteArrayOf(
            'O'.code.toByte(), 'p'.code.toByte(), 'u'.code.toByte(), 's'.code.toByte(),
            'H'.code.toByte(), 'e'.code.toByte(), 'a'.code.toByte(), 'd'.code.toByte(),
            1, 1,  // version=1, channels=1
            0x20, 0x03,  // pre-skip = 800
            0x00, 0x00, 0x00, 0x00,  // input sample rate = 0 (unspecified)
            0x00, 0x00,  // output gain = 0
            0   // channel mapping family = 0
        )

        val header = OpusIdentificationHeader(data)
        assertTrue(header.isValid)
        assertEquals(1, header.channelNumber)
        assertEquals(800, header.preSkip)
        assertEquals(0, header.inputSampleRate)
        assertEquals(1, header.streamCount) // Mono = 1 stream
        assertEquals(0, header.coupledCount) // Mono = no coupling
    }

    @Test
    fun `reject invalid OpusHead magic`() {
        // "vorbis" instead of "OpusHead" at offset 0
        val data = byteArrayOf(
            'v'.code.toByte(), 'o'.code.toByte(), 'r'.code.toByte(), 'b'.code.toByte(),
            'i'.code.toByte(), 's'.code.toByte(), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        )
        val header = OpusIdentificationHeader(data)
        assertTrue(!header.isValid)
    }

    @Test
    fun `isOpusIdentificationHeader detects OpusHead at offset 0`() {
        val opusHead = byteArrayOf(
            'O'.code.toByte(), 'p'.code.toByte(), 'u'.code.toByte(), 's'.code.toByte(),
            'H'.code.toByte(), 'e'.code.toByte(), 'a'.code.toByte(), 'd'.code.toByte(),
            1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        )
        assertTrue(OpusIdentificationHeader.isOpusIdentificationHeader(opusHead))

        // Too short (< 8 bytes for magic)
        assertTrue(!OpusIdentificationHeader.isOpusIdentificationHeader(byteArrayOf('O'.code.toByte(), 'p'.code.toByte())))

        // Wrong magic: "OpusTags" is NOT an OpusHead
        val opusTags = byteArrayOf(
            'O'.code.toByte(), 'p'.code.toByte(), 'u'.code.toByte(), 's'.code.toByte(),
            'T'.code.toByte(), 'a'.code.toByte(), 'g'.code.toByte(), 's'.code.toByte()
        )
        assertTrue(!OpusIdentificationHeader.isOpusIdentificationHeader(opusTags))
    }

    @Test
    fun `OpusHead size constants are correct`() {
        // 8 (magic) + 1 + 1 + 2 + 4 + 2 + 1 = 19
        assertEquals(19, OpusIdentificationHeader.OPUS_HEAD_MIN_SIZE)
        assertEquals(8, OpusIdentificationHeader.OPUS_HEAD_MAGIC_LENGTH)
        assertEquals(8, OpusIdentificationHeader.OPUS_TAGS_MAGIC_LENGTH)
    }
}
