package com.moriafly.jaudiotagger

import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.SupportedFileFormat
import org.jaudiotagger.audio.aac.AdtsHeader
import org.jaudiotagger.audio.exceptions.CannotReadException
import org.jaudiotagger.audio.exceptions.CannotWriteException
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.id3.AbstractID3v2Tag
import org.jaudiotagger.tag.id3.ID3v22Tag
import org.jaudiotagger.tag.id3.ID3v23Tag
import org.jaudiotagger.utils.FileTypeUtil
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AacTest {
    @Test
    fun `aac supported file format exists`() {
        val format = SupportedFileFormat.valueOf("AAC")
        assertEquals("aac", format.filesuffix)
        assertEquals("Aac", format.displayName)
    }

    @Test
    fun `parses ADTS header fields`() {
        val header = assertNotNull(
            AdtsHeader.parse(
                adtsFrame(
                    payloadSize = 20,
                    sampleRateIndex = 3,
                    profile = 0,
                    channelConfiguration = 6,
                    protectionAbsent = false,
                    bufferFullness = 0x123,
                    rawDataBlocks = 4,
                    mpegVersion = 1
                )
            )
        )

        assertEquals(1, header.mpegVersion)
        assertEquals(0, header.profile)
        assertEquals("AAC Main", header.profileName)
        assertEquals(48000, header.sampleRate)
        assertEquals(6, header.channelConfiguration)
        assertEquals(6, header.channelCount)
        assertEquals(29, header.frameLength)
        assertEquals(9, header.headerLength)
        assertEquals(0x123, header.bufferFullness)
        assertEquals(4, header.rawDataBlockCount)
        assertFalse(header.isVariableBitRate)
    }

    @Test
    fun `rejects invalid ADTS headers`() {
        assertNull(AdtsHeader.parse(null))
        assertNull(AdtsHeader.parse(ByteArray(AdtsHeader.MIN_HEADER_LENGTH - 1)))

        val badSync = adtsFrame(payloadSize = 20).apply { this[0] = 0 }
        assertNull(AdtsHeader.parse(badSync))

        val badLayer = adtsFrame(payloadSize = 20).apply {
            this[1] = (this[1].toInt() or 0x02).toByte()
        }
        assertNull(AdtsHeader.parse(badLayer))

        for (sampleRateIndex in 13..15) {
            val frame = adtsFrame(payloadSize = 20, sampleRateIndex = sampleRateIndex)
            assertNull(AdtsHeader.parse(frame))
        }

        val undersizedFrame = adtsFrame(payloadSize = 0, protectionAbsent = false)
        setFrameLength(undersizedFrame, AdtsHeader.CRC_HEADER_LENGTH - 1)
        assertNull(AdtsHeader.parse(undersizedFrame))
    }

    @Test
    fun `compares ADTS audio configurations`() {
        val base = assertNotNull(AdtsHeader.parse(adtsFrame(payloadSize = 20)))
        assertEquals(AdtsHeader.MIN_HEADER_LENGTH, base.headerLength)
        assertEquals("AAC LC", base.profileName)
        assertEquals(2, base.channelCount)
        assertTrue(base.isVariableBitRate)

        val sameConfiguration = assertNotNull(
            AdtsHeader.parse(adtsFrame(payloadSize = 40, bufferFullness = 0x123))
        )
        assertTrue(base.hasSameAudioConfiguration(sameConfiguration))
        assertFalse(base.hasSameAudioConfiguration(null))
        assertFalse(
            base.hasSameAudioConfiguration(
                AdtsHeader.parse(adtsFrame(payloadSize = 20, mpegVersion = 1))
            )
        )
        assertFalse(
            base.hasSameAudioConfiguration(
                AdtsHeader.parse(adtsFrame(payloadSize = 20, profile = 2))
            )
        )
        assertFalse(
            base.hasSameAudioConfiguration(
                AdtsHeader.parse(adtsFrame(payloadSize = 20, sampleRateIndex = 3))
            )
        )
        assertFalse(
            base.hasSameAudioConfiguration(
                AdtsHeader.parse(adtsFrame(payloadSize = 20, channelConfiguration = 1))
            )
        )
    }

    @Test
    fun `reads ADTS AAC with a leading ID3v24 tag`() {
        val frames = defaultFrames()
        withTestFile(tag = id3v24Tag(), frames = frames) { file ->
            assertEquals("AAC", FileTypeUtil.getMagicFileType(file))
            assertEquals("aac", FileTypeUtil.getMagicExt("AAC"))

            val audioFile = AudioFileIO.read(file)
            val header = audioFile.audioHeader
            assertEquals("aac", audioFile.ext)
            assertEquals("AAC", header.format)
            assertEquals("AAC LC", header.encodingType)
            assertEquals(44100, header.sampleRateAsNumber)
            assertEquals("2", header.channels)
            assertEquals(16, header.bitsPerSample)
            assertEquals(frames.size * 1024L, header.noOfSamples)
            assertEquals(37L, header.audioDataStartPosition)
            assertEquals(file.length(), header.audioDataEndPosition)
            assertEquals(file.length() - 37L, header.audioDataLength)
            assertTrue(header.bitRateAsNumber > 0)
            assertTrue(header.byteRate > 0)
            assertTrue(header.isVariableBitRate)
            assertFalse(header.isLossless)
            assertTrue(
                abs(header.preciseTrackLength - frames.size * 1024.0 / 44100.0) < 0.000001
            )
            assertEquals("2024", audioFile.tag.getFirst(FieldKey.YEAR))
            assertEquals("2", audioFile.tag.getFirst(FieldKey.TRACK))

            val magicAudioFile = AudioFileIO.readMagic(file)
            assertEquals("aac", magicAudioFile.ext)
            assertEquals("AAC", magicAudioFile.audioHeader.format)
        }
    }

    @Test
    fun `reads CRC protected CBR frames with multiple raw data blocks`() {
        val frames = List(4) {
            adtsFrame(
                payloadSize = 40,
                protectionAbsent = false,
                bufferFullness = 0x123,
                rawDataBlocks = 4
            )
        }
        withTestFile(tag = null, frames = frames) { file ->
            val header = AudioFileIO.read(file).audioHeader
            assertEquals(frames.size * 4L * 1024L, header.noOfSamples)
            assertEquals(0L, header.audioDataStartPosition)
            assertEquals(16, header.bitsPerSample)
            assertFalse(header.isVariableBitRate)
        }
    }

    @Test
    fun `reads empty ID3v22 and ID3v23 tags`() {
        withTestFile(tag = emptyId3Tag(version = 2), frames = defaultFrames()) { file ->
            assertIs<ID3v22Tag>(AudioFileIO.read(file).tag)
        }
        withTestFile(tag = emptyId3Tag(version = 3), frames = defaultFrames()) { file ->
            assertIs<ID3v23Tag>(AudioFileIO.read(file).tag)
        }
    }

    @Test
    fun `skips an ID3v24 footer`() {
        val tag = id3v24Tag(withFooter = true)
        withTestFile(tag = tag, frames = defaultFrames()) { file ->
            assertEquals(47L, AbstractID3v2Tag.getV2TagSizeIfExists(file))
            val audioFile = AudioFileIO.read(file)
            assertEquals(47L, audioFile.audioHeader.audioDataStartPosition)
            assertEquals("2024", audioFile.tag.getFirst(FieldKey.YEAR))
            assertEquals("2", audioFile.tag.getFirst(FieldKey.TRACK))
            assertEquals("AAC", FileTypeUtil.getMagicFileType(file))
        }
    }

    @Test
    fun `magic detection recognizes ADTS AAC without ID3`() {
        withTestFile(tag = null, frames = defaultFrames()) { file ->
            assertEquals("AAC", FileTypeUtil.getMagicFileType(file))
            val audioFile = AudioFileIO.readMagic(file)
            assertEquals("aac", audioFile.ext)
            assertEquals(0, audioFile.tag.fieldCount)
            assertEquals(0L, audioFile.audioHeader.audioDataStartPosition)
        }
    }

    @Test
    fun `magic detection recognizes one complete ADTS frame`() {
        withTestFile(tag = null, frames = listOf(adtsFrame(payloadSize = 120))) { file ->
            assertEquals("AAC", FileTypeUtil.getMagicFileType(file))
            assertEquals(1024L, AudioFileIO.readMagic(file).audioHeader.noOfSamples)
        }
    }

    @Test
    fun `magic detection rejects an isolated ADTS-like header`() {
        withTestFile(
            tag = null,
            frames = listOf(adtsFrame(payloadSize = 120)),
            trailingData = ByteArray(100)
        ) { file ->
            assertEquals("UNKNOWN", FileTypeUtil.getMagicFileType(file))
        }
    }

    @Test
    fun `magic detection rejects a truncated second ADTS frame`() {
        val firstFrame = adtsFrame(payloadSize = 120)
        val secondFrameHeader = adtsFrame(payloadSize = 120).copyOf(AdtsHeader.MIN_HEADER_LENGTH)
        withTestFile(tag = null, frames = listOf(firstFrame, secondFrameHeader)) { file ->
            assertEquals("UNKNOWN", FileTypeUtil.getMagicFileType(file))
        }
    }

    @Test
    fun `ID3 signature without ADTS remains classified as MP3`() {
        withTestFile(tag = id3v24Tag(), frames = emptyList(), trailingData = ByteArray(128)) {
            file ->
            assertEquals("MP3IDv2", FileTypeUtil.getMagicFileType(file))
            assertEquals("mp3", FileTypeUtil.getMagicExt("MP3IDv2"))
        }
    }

    @Test
    fun `rejects a truncated ADTS frame`() {
        val frames = defaultFrames().toMutableList()
        frames[frames.lastIndex] = frames.last().copyOf(frames.last().size - 1)
        withTestFile(tag = id3v24Tag(), frames = frames) { file ->
            val exception = assertFailsWith<CannotReadException> {
                AudioFileIO.read(file)
            }
            assertContains(exception.message.orEmpty(), "truncated ADTS frame")
        }
    }

    @Test
    fun `rejects a truncated ADTS header`() {
        withTestFile(
            tag = null,
            frames = defaultFrames(),
            trailingData = byteArrayOf(0xFF.toByte(), 0xF1.toByte(), 0x50)
        ) { file ->
            val exception = assertFailsWith<CannotReadException> {
                AudioFileIO.read(file)
            }
            assertContains(exception.message.orEmpty(), "truncated ADTS header")
        }
    }

    @Test
    fun `rejects an invalid ADTS header between frames`() {
        val frames = defaultFrames().toMutableList()
        frames[2] = frames[2].apply { this[0] = 0 }
        withTestFile(tag = null, frames = frames) { file ->
            val exception = assertFailsWith<CannotReadException> {
                AudioFileIO.read(file)
            }
            assertContains(exception.message.orEmpty(), "invalid ADTS header")
        }
    }

    @Test
    fun `rejects ADTS configuration changes`() {
        val frames = listOf(
            adtsFrame(payloadSize = 40, sampleRateIndex = 4),
            adtsFrame(payloadSize = 40, sampleRateIndex = 3),
            adtsFrame(payloadSize = 40, sampleRateIndex = 4)
        )
        withTestFile(tag = null, frames = frames) { file ->
            assertEquals("UNKNOWN", FileTypeUtil.getMagicFileType(file))
            val exception = assertFailsWith<CannotReadException> {
                AudioFileIO.read(file)
            }
            assertContains(exception.message.orEmpty(), "changes ADTS audio configuration")
        }
    }

    @Test
    fun `rejects ADTS program configuration elements`() {
        val frames = List(3) {
            adtsFrame(payloadSize = 40, channelConfiguration = 0)
        }
        withTestFile(tag = null, frames = frames) { file ->
            val exception = assertFailsWith<CannotReadException> {
                AudioFileIO.read(file)
            }
            assertContains(exception.message.orEmpty(), "program configuration element")
        }
    }

    @Test
    fun `rejects an ID3 tag without AAC audio`() {
        withTestFile(
            tag = emptyId3Tag(version = 4, payloadSize = 120),
            frames = emptyList()
        ) { file ->
            val exception = assertFailsWith<CannotReadException> {
                AudioFileIO.read(file)
            }
            assertContains(exception.message.orEmpty(), "does not contain ADTS audio")
        }
    }

    @Test
    fun `AAC writing remains unsupported`() {
        withTestFile(tag = null, frames = defaultFrames()) { file ->
            val audioFile = AudioFileIO.read(file)
            assertFailsWith<CannotWriteException> {
                AudioFileIO.write(audioFile)
            }
        }
    }

    private fun withTestFile(
        tag: ByteArray?,
        frames: List<ByteArray>,
        trailingData: ByteArray = byteArrayOf(),
        block: (File) -> Unit
    ) {
        val path = Files.createTempFile("jaudiotagger-adts-", ".aac")
        try {
            val output = ByteArrayOutputStream()
            tag?.let(output::write)
            frames.forEach(output::write)
            output.write(trailingData)
            Files.write(path, output.toByteArray())
            block(path.toFile())
        } finally {
            Files.deleteIfExists(path)
        }
    }

    private fun defaultFrames(): List<ByteArray> =
        List(8) { index -> adtsFrame(payloadSize = 40 + index) }

    private fun adtsFrame(
        payloadSize: Int,
        sampleRateIndex: Int = 4,
        profile: Int = 1,
        channelConfiguration: Int = 2,
        protectionAbsent: Boolean = true,
        bufferFullness: Int = 0x7FF,
        rawDataBlocks: Int = 1,
        mpegVersion: Int = 0
    ): ByteArray {
        val headerLength =
            if (protectionAbsent) AdtsHeader.MIN_HEADER_LENGTH else AdtsHeader.CRC_HEADER_LENGTH
        val frameLength = headerLength + payloadSize
        require(frameLength <= 0x1FFF)
        require(rawDataBlocks in 1..4)

        val frame = ByteArray(frameLength)
        frame[0] = 0xFF.toByte()
        frame[1] = (
            0xF0 or ((mpegVersion and 0x01) shl 3) or if (protectionAbsent) 1 else 0
        ).toByte()
        frame[2] = (
            (profile shl 6) or (sampleRateIndex shl 2) or (channelConfiguration ushr 2)
        ).toByte()
        frame[3] = (
            ((channelConfiguration and 0x03) shl 6) or (frameLength ushr 11)
        ).toByte()
        frame[4] = (frameLength ushr 3).toByte()
        frame[5] = (
            ((frameLength and 0x07) shl 5) or (bufferFullness ushr 6)
        ).toByte()
        frame[6] = (
            ((bufferFullness and 0x3F) shl 2) or (rawDataBlocks - 1)
        ).toByte()
        for (index in headerLength until frame.size) {
            frame[index] = (index and 0x7F).toByte()
        }
        return frame
    }

    private fun setFrameLength(frame: ByteArray, frameLength: Int) {
        frame[3] = ((frame[3].toInt() and 0xFC) or (frameLength ushr 11)).toByte()
        frame[4] = (frameLength ushr 3).toByte()
        frame[5] = (
            (frame[5].toInt() and 0x1F) or ((frameLength and 0x07) shl 5)
        ).toByte()
    }

    private fun id3v24Tag(withFooter: Boolean = false): ByteArray {
        val body = byteArrayOf(
            0x54, 0x44, 0x52, 0x43, 0x00, 0x00, 0x00, 0x05, 0x00, 0x00,
            0x03, 0x32, 0x30, 0x32, 0x34,
            0x54, 0x52, 0x43, 0x4B, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00,
            0x03, 0x32
        )
        val flags = if (withFooter) 0x10 else 0
        val size = syncSafe(body.size)
        val header = byteArrayOf(0x49, 0x44, 0x33, 0x04, 0x00, flags.toByte()) + size
        val footer = if (withFooter) {
            byteArrayOf(0x33, 0x44, 0x49, 0x04, 0x00, flags.toByte()) + size
        } else {
            byteArrayOf()
        }
        return header + body + footer
    }

    private fun emptyId3Tag(version: Int, payloadSize: Int = 0): ByteArray =
        byteArrayOf(0x49, 0x44, 0x33, version.toByte(), 0x00, 0x00) +
            syncSafe(payloadSize) +
            ByteArray(payloadSize)

    private fun syncSafe(value: Int): ByteArray = byteArrayOf(
        ((value ushr 21) and 0x7F).toByte(),
        ((value ushr 14) and 0x7F).toByte(),
        ((value ushr 7) and 0x7F).toByte(),
        (value and 0x7F).toByte()
    )
}
