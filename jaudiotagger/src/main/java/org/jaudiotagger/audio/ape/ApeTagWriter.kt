package org.jaudiotagger.audio.ape

import org.jaudiotagger.audio.exceptions.CannotReadException
import org.jaudiotagger.audio.exceptions.CannotWriteException
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.ape.ApeTag
import org.jaudiotagger.tag.ape.ApeTagCreator
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class ApeTagWriter {

    private val creator = ApeTagCreator()

    @Throws(CannotReadException::class, CannotWriteException::class)
    fun delete(tag: Tag, path: Path) {
        val existingTagSize = findExistingTagSize(path)
        if (existingTagSize > 0) {
            val channel = FileChannel.open(path, StandardOpenOption.WRITE)
            channel.use {
                val currentSize = channel.size()
                channel.truncate(currentSize - existingTagSize)
            }
        }
    }

    @Throws(CannotWriteException::class)
    fun write(tag: Tag, path: Path) {
        try {
            delete(tag, path)

            val tagBytes = creator.convert(tag as ApeTag)
            val channel = FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.APPEND)
            channel.use {
                channel.position(channel.size())
                channel.write(tagBytes)
            }
        } catch (e: IOException) {
            throw CannotWriteException("Failed to write APE tag: ${e.message}")
        } catch (e: CannotReadException) {
            throw CannotWriteException("Failed to delete old APE tag: ${e.message}")
        }
    }

    private fun findExistingTagSize(path: Path): Long {
        val fileLength = Files.size(path)
        if (fileLength < ApeTagReader.FOOTER_SIZE) return 0

        val channel = FileChannel.open(path, StandardOpenOption.READ)
        channel.use {
            channel.position(fileLength - ApeTagReader.FOOTER_SIZE)
            val footer = ByteBuffer.allocate(ApeTagReader.FOOTER_SIZE)
            channel.read(footer)
            footer.flip()

            val preamble = ByteArray(8)
            footer.get(preamble)
            if (preamble.decodeToString() != ApeTagReader.FOOTER_PREAMBLE) {
                return 0
            }

            footer.position(12)
            val tagSize = footer.int
            val flags = footer.int

            val hasHeader = (flags and ApeTagReader.FLAG_CONTAINS_HEADER) != 0
            val isHeader = (flags and ApeTagReader.FLAG_IS_HEADER) != 0

            if (isHeader) return 0

            return if (hasHeader) {
                tagSize.toLong() + ApeTagReader.FOOTER_SIZE
            } else {
                tagSize.toLong()
            }
        }
    }
}
