package com.moriafly.jaudiotagger

import org.jaudiotagger.audio.wav.WavOptions
import org.jaudiotagger.audio.wav.chunk.WavInfoChunk
import org.jaudiotagger.audio.wav.chunk.WavInfoIdentifier
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.TagOptionSingleton
import org.jaudiotagger.tag.wav.WavTag
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(UnstableJaudiotaggerApi::class)
class WavInfoChunkTest {
    private val options = TagOptionSingleton.getInstance()

    @BeforeTest
    fun resetOptionsBeforeTest() {
        options.setToDefault()
        JaudiotaggerFlags.wavInfoFallbackCharset = null
    }

    @AfterTest
    fun resetOptionsAfterTest() {
        options.setToDefault()
        JaudiotaggerFlags.wavInfoFallbackCharset = null
    }

    @Test
    fun `valid UTF-8 is decoded strictly and terminators are removed`() {
        val tag = readChunk(
            field("INAM", terminated("胜景赛娑婆", StandardCharsets.UTF_8)),
            field("IART", terminated("游戏科学 & 8082Audio", StandardCharsets.UTF_8)),
        )

        assertEquals("胜景赛娑婆", tag.infoTag.getFirst(FieldKey.TITLE))
        assertEquals("游戏科学 & 8082Audio", tag.infoTag.getFirst(FieldKey.ARTIST))
    }

    @Test
    fun `GB18030 fallback is selected consistently for a legacy INFO chunk`() {
        val gb18030 = Charset.forName("GB18030")
        JaudiotaggerFlags.wavInfoFallbackCharset = gb18030

        val tag = readChunk(
            field("INAM", terminated("胜景赛娑婆", gb18030)),
            field("IART", terminated("游戏科学 & 8082Audio", gb18030)),
            field("TRCK", terminated("2/18", StandardCharsets.US_ASCII)),
            field("IAAT", terminated("游戏科学 & 8082Audio", gb18030)),
        )

        assertEquals("胜景赛娑婆", tag.infoTag.getFirst(FieldKey.TITLE))
        assertEquals("游戏科学 & 8082Audio", tag.infoTag.getFirst(FieldKey.ARTIST))
        assertEquals("2/18", tag.infoTag.getFirst(FieldKey.TRACK))
        assertEquals("游戏科学 & 8082Audio", tag.infoTag.getFirst(FieldKey.ALBUM_ARTIST))
    }

    @Test
    fun `chunk-wide override handles ambiguous bytes and unknown fields`() {
        val gb18030 = Charset.forName("GB18030")
        // GB18030 encodes 漏 as C2 A9, which is also valid UTF-8 for ©.
        // Detection cannot disambiguate this case, so a forced override must win.
        options.overrideCharset = gb18030
        options.isOverrideCharsetForInfo = true

        val tag = readChunk(
            field("INAM", terminated("漏", gb18030)),
            field("XVEN", terminated("漏", gb18030)),
        )

        assertEquals("漏", tag.infoTag.getFirst(FieldKey.TITLE))
        assertEquals("漏", tag.unrecognized("XVEN"))
    }

    @Test
    fun `selective override does not dereference an unknown INFO identifier`() {
        val gb18030 = Charset.forName("GB18030")
        options.overrideCharset = gb18030
        options.isOverrideCharsetForInfo = true
        options.addOverrideCharsetFields(FieldKey.TITLE)

        val tag = readChunk(
            field("INAM", terminated("胜景", gb18030)),
            field("XVEN", terminated("vendor field", StandardCharsets.UTF_8)),
        )

        assertEquals("胜景", tag.infoTag.getFirst(FieldKey.TITLE))
        assertEquals("vendor field", tag.unrecognized("XVEN"))
    }

    @Test
    fun `read aliases do not replace canonical identifiers used for writing`() {
        assertEquals("ITRK", WavInfoIdentifier.getByFieldKey(FieldKey.TRACK).code)
        assertEquals("iaar", WavInfoIdentifier.getByFieldKey(FieldKey.ALBUM_ARTIST).code)
    }

    @Test
    fun `a BOM takes precedence over automatic UTF-8 detection`() {
        val utf16Value = byteArrayOf(0xff.toByte(), 0xfe.toByte()) +
            "标题".toByteArray(StandardCharsets.UTF_16LE) +
            byteArrayOf(0, 0)

        val tag = readChunk(field("INAM", utf16Value))

        assertEquals("标题", tag.infoTag.getFirst(FieldKey.TITLE))
    }

    @Test
    fun `invalid UTF-8 is preserved losslessly when no fallback is configured`() {
        val gb18030 = Charset.forName("GB18030")
        val rawValue = "胜景".toByteArray(gb18030)

        val tag = readChunk(field("INAM", rawValue + byteArrayOf(0)))

        assertEquals(
            String(rawValue, StandardCharsets.ISO_8859_1),
            tag.infoTag.getFirst(FieldKey.TITLE),
        )
        assertFalse(tag.infoTag.getFirst(FieldKey.TITLE).contains('\uFFFD'))
    }

    @Test
    fun `malformed field size fails without exposing a partial tag`() {
        val validField = field("INAM", terminated("partial", StandardCharsets.UTF_8))
        val malformedHeader = "IART".toByteArray(StandardCharsets.US_ASCII) + littleEndian(100)
        val buffer = ByteBuffer.wrap(validField + malformedHeader + byteArrayOf(1, 2))
            .order(ByteOrder.LITTLE_ENDIAN)
        val tag = WavTag(WavOptions.READ_INFO_ONLY)

        assertFalse(WavInfoChunk(tag, "test:").readChunks(buffer))
        assertTrue(tag.infoTag.isEmpty)
    }

    private fun readChunk(vararg fields: ByteArray): WavTag {
        val buffer = ByteBuffer.wrap(fields.fold(byteArrayOf()) { result, field -> result + field })
            .order(ByteOrder.LITTLE_ENDIAN)
        val tag = WavTag(WavOptions.READ_INFO_ONLY)

        assertTrue(WavInfoChunk(tag, "test:").readChunks(buffer))
        return tag
    }

    private fun field(id: String, value: ByteArray): ByteArray {
        require(id.length == 4)
        val output = ByteArrayOutputStream()
        output.write(id.toByteArray(StandardCharsets.US_ASCII))
        output.write(littleEndian(value.size))
        output.write(value)
        if (value.size % 2 != 0) {
            output.write(0)
        }
        return output.toByteArray()
    }

    private fun terminated(value: String, charset: Charset): ByteArray =
        value.toByteArray(charset) + byteArrayOf(0)

    private fun littleEndian(value: Int): ByteArray = byteArrayOf(
        value.toByte(),
        (value ushr 8).toByte(),
        (value ushr 16).toByte(),
        (value ushr 24).toByte(),
    )

    private fun WavTag.unrecognized(id: String): String =
        infoTag.unrecognisedFields.first { it.id == id }.content
}
