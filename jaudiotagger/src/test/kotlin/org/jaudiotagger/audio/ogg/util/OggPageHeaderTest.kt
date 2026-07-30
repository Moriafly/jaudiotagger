package org.jaudiotagger.audio.ogg.util

import java.io.RandomAccessFile
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class OggPageHeaderTest {
    @Test
    fun `readLast finds the final page across search buffer boundaries`() {
        val file = Files.createTempFile("jaudiotagger-ogg-page-", ".ogg")
        try {
            RandomAccessFile(file.toFile(), "rw").use { randomAccessFile ->
                val fileLength = 128 * 1024L
                val searchBufferBoundary = fileLength - 1 - 64 * 1024
                val firstPagePosition = 128L
                val lastPagePosition = searchBufferBoundary - 2

                randomAccessFile.setLength(fileLength)
                randomAccessFile.seek(firstPagePosition)
                randomAccessFile.write(createPageHeader(1_000))
                randomAccessFile.seek(lastPagePosition)
                randomAccessFile.write(createPageHeader(2_000))

                val pageHeader = OggPageHeader.readLast(randomAccessFile)

                assertEquals(lastPagePosition, pageHeader.startByte)
                assertEquals(2_000.0, pageHeader.absoluteGranulePosition)
            }
        } finally {
            Files.deleteIfExists(file)
        }
    }

    private fun createPageHeader(granulePosition: Long): ByteArray =
        ByteArray(OggPageHeader.OGG_PAGE_HEADER_FIXED_LENGTH).also { header ->
            OggPageHeader.CAPTURE_PATTERN.copyInto(header)
            repeat(OggPageHeader.FIELD_ABSOLUTE_GRANULE_LENGTH) { index ->
                header[OggPageHeader.FIELD_ABSOLUTE_GRANULE_POS + index] =
                    (granulePosition shr (index * 8)).toByte()
            }
        }
}
