package org.jaudiotagger.audio.ape

import org.jaudiotagger.audio.exceptions.CannotReadException
import org.jaudiotagger.tag.ape.ApeTag
import org.jaudiotagger.tag.ape.ApeTagField
import org.jaudiotagger.tag.ape.ApeTagFieldBinary
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.SeekableByteChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.logging.Logger

class ApeTagReader {

    @Throws(CannotReadException::class, IOException::class)
    fun read(path: Path): ApeTag {
        val fileLength = Files.size(path)
        if (fileLength < FOOTER_SIZE) {
            return ApeTag()
        }

        val channel = Files.newByteChannel(path)
        channel.use {
            val footer = readFooter(channel, fileLength)
                ?: return ApeTag()

            val version = footer.int
            if (version > 2000) {
                return ApeTag()
            }

            val tagSize = footer.int and 0x7FFFFFFF
            val itemCount = footer.int and 0x7FFFFFFF
            val flags = footer.int

            val hasHeader = (flags and FLAG_CONTAINS_HEADER) != 0
            val isHeader = (flags and FLAG_IS_HEADER) != 0

            if (isHeader) {
                return ApeTag()
            }

            val footerPosition = fileLength - FOOTER_SIZE
            // tagSize = items + footer, excludes header
            // With header: total = header(32) + items + footer(32) = tagSize + 32
            // Items start at: F - (tagSize + 32) + 32 = F - tagSize
            // Without header: total = items + footer = tagSize
            // Items start at: F - tagSize
            val tagStart = fileLength - tagSize

            if (tagStart < 0 || tagStart > fileLength) {
                return ApeTag()
            }

            channel.position(tagStart)
            val itemDataSize = tagSize - FOOTER_SIZE
            if (itemDataSize <= 0 || itemDataSize > fileLength) {
                return ApeTag()
            }

            val itemBuffer = ByteBuffer.allocate(itemDataSize).order(ByteOrder.LITTLE_ENDIAN)
            channel.read(itemBuffer)
            itemBuffer.flip()

            return parseItems(itemBuffer, itemCount)
        }
    }

    private fun readFooter(channel: SeekableByteChannel, fileLength: Long): ByteBuffer? {
        channel.position(fileLength - FOOTER_SIZE)
            val footer = ByteBuffer.allocate(FOOTER_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        channel.read(footer)
        footer.flip()

        val preamble = ByteArray(8)
        footer.get(preamble)
        if (preamble.decodeToString() != FOOTER_PREAMBLE) {
            return null
        }

        return footer
    }

    private fun parseItems(buffer: ByteBuffer, itemCount: Int): ApeTag {
        val tag = ApeTag()

        for (i in 0 until itemCount) {
            if (buffer.remaining() < 8) break

            val valueSize = buffer.int
            val itemFlags = buffer.int

            val keyBuilder = StringBuilder()
            while (buffer.hasRemaining()) {
                val b = buffer.get().toInt() and 0xFF
                if (b == 0) break
                if (b in 0x20..0x7E) {
                    keyBuilder.append(b.toChar())
                }
            }

            val key = keyBuilder.toString()
            if (key.isEmpty() || key.length < 2) {
                if (buffer.remaining() >= valueSize) {
                    buffer.position(buffer.position() + valueSize)
                }
                continue
            }

            val itemType = (itemFlags shr 1) and 0x3
            val valueBytes = ByteArray(valueSize)
            if (buffer.remaining() >= valueSize) {
                buffer.get(valueBytes)
            } else {
                break
            }

            when (itemType) {
                ITEM_TYPE_TEXT -> {
                    val value = String(valueBytes, StandardCharsets.UTF_8)
                    tag.addField(ApeTagField(key, value))
                }
                ITEM_TYPE_BINARY -> {
                    val nullIndex = valueBytes.indexOf(0x00)
                    if (nullIndex >= 0) {
                        val description = String(valueBytes, 0, nullIndex, StandardCharsets.UTF_8)
                        val imageData = valueBytes.copyOfRange(nullIndex + 1, valueBytes.size)
                        tag.addField(ApeTagFieldBinary(key, description, imageData))
                    } else {
                        tag.addField(ApeTagFieldBinary(key, "", valueBytes))
                    }
                }
            }
        }

        return tag
    }

    companion object {
        private val logger = Logger.getLogger("org.jaudiotagger.audio.ape")

        const val FOOTER_SIZE = 32
        const val FOOTER_PREAMBLE = "APETAGEX"
        const val FLAG_CONTAINS_HEADER = 1 shl 31
        const val FLAG_IS_HEADER = 1 shl 29
        const val ITEM_TYPE_TEXT = 0
        const val ITEM_TYPE_BINARY = 1
    }
}
