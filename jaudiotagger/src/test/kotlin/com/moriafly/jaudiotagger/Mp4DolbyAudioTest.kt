package com.moriafly.jaudiotagger

import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.mp4.Mp4FieldKey
import org.jaudiotagger.tag.mp4.field.Mp4FieldType
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Mp4DolbyAudioTest {
    @Test
    fun `reads E-AC-3 metadata and artwork`() {
        assertDolbyFileCanBeRead("ec-3", "E-AC-3")
    }

    @Test
    fun `reads AC-4 metadata and artwork`() {
        assertDolbyFileCanBeRead("ac-4", "AC-4")
    }

    private fun assertDolbyFileCanBeRead(codec: String, expectedEncoding: String) {
        val cover = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte())
        val file = Files.createTempFile("jaudiotagger-dolby-", ".m4a")
        try {
            Files.write(file, createMp4(codec, cover))

            val audioFile = AudioFileIO.read(file.toFile())

            assertEquals(expectedEncoding, audioFile.audioHeader.encodingType)
            assertTrue(audioFile.audioHeader.bitRateAsNumber > 0)
            assertEquals("Time", audioFile.tag.getFirst(FieldKey.TITLE))
            assertEquals("image/jpeg", audioFile.tag.firstArtwork.mimeType)
            assertContentEquals(cover, audioFile.tag.firstArtwork.binaryData)
        } finally {
            Files.deleteIfExists(file)
        }
    }

    private fun createMp4(codec: String, cover: ByteArray): ByteArray {
        val ftyp = box("ftyp", ints(fourCc("M4A "), 0, fourCc("isom")))
        val mvhd = box("mvhd", timingPayload())
        val mdhd = box("mdhd", timingPayload())
        val sampleEntry = box(codec, ByteArray(28))
        val stsd = box("stsd", ints(0, 1), sampleEntry)
        val stco = box("stco", ints(0, 1, 1))
        val stbl = box("stbl", stsd, stco)
        val minf = box("minf", box("smhd", ints(0, 0)), stbl)
        val trak = box("trak", box("mdia", mdhd, minf))

        val title = metadataField(
            Mp4FieldKey.TITLE.fieldName,
            Mp4FieldType.TEXT.fileClassId,
            "Time".toByteArray(StandardCharsets.UTF_8),
        )
        val artwork = metadataField(
            Mp4FieldKey.ARTWORK.fieldName,
            Mp4FieldType.COVERART_JPEG.fileClassId,
            cover,
        )
        val meta = box("meta", ints(0), box("ilst", title, artwork))
        val moov = box("moov", mvhd, trak, box("udta", meta))
        val mdat = box("mdat", ByteArray(2_048))
        return ftyp + moov + mdat
    }

    private fun metadataField(id: String, type: Int, data: ByteArray): ByteArray =
        box(id, box("data", ints(type, 0), data))

    private fun timingPayload(): ByteArray = ints(0, 0, 0, 1_000, 10_000)

    private fun ints(vararg values: Int): ByteArray =
        ByteBuffer.allocate(values.size * Int.SIZE_BYTES).apply {
            values.forEach(::putInt)
        }.array()

    private fun fourCc(value: String): Int = ByteBuffer.wrap(
        value.toByteArray(StandardCharsets.ISO_8859_1),
    ).int

    private fun box(id: String, vararg payloads: ByteArray): ByteArray {
        val idBytes = id.toByteArray(StandardCharsets.ISO_8859_1)
        require(idBytes.size == 4)
        return ByteBuffer.allocate(8 + payloads.sumOf(ByteArray::size)).apply {
            putInt(capacity())
            put(idBytes)
            payloads.forEach(::put)
        }.array()
    }
}
