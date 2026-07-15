package org.jaudiotagger.tag.ape

import org.jaudiotagger.tag.TagField
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

class ApeTagCreator {

    fun convert(tag: ApeTag): ByteBuffer {
        val allFields = mutableListOf<TagField>()
        val fieldsIterator = tag.fields
        while (fieldsIterator.hasNext()) {
            allFields.add(fieldsIterator.next())
        }

        val itemBuffers = mutableListOf<ByteArray>()
        for (field in allFields) {
            itemBuffers.add(field.getRawContent())
        }

        val totalItemSize = itemBuffers.sumOf { it.size }
        val tagSize = totalItemSize + FOOTER_SIZE
        val itemCount = allFields.size

        val buffer = ByteBuffer.allocate(totalItemSize + FOOTER_SIZE).order(ByteOrder.LITTLE_ENDIAN)

        for (itemBytes in itemBuffers) {
            buffer.put(itemBytes)
        }

        writeFooter(buffer, tagSize, itemCount)

        buffer.flip()
        val result = ByteArray(buffer.remaining())
        buffer.get(result)
        return ByteBuffer.wrap(result)
    }

    private fun writeFooter(buffer: ByteBuffer, tagSize: Int, itemCount: Int) {
        val preamble = FOOTER_PREAMBLE.toByteArray(StandardCharsets.US_ASCII)
        buffer.put(preamble)
        buffer.putInt(APE_TAG_VERSION)
        buffer.putInt(tagSize)
        buffer.putInt(itemCount)
        buffer.putInt(FLAG_CONTAINS_HEADER)
        buffer.putLong(0)
    }

    companion object {
        const val FOOTER_SIZE = 32
        const val APE_TAG_VERSION = 2000
        const val FOOTER_PREAMBLE = "APETAGEX"
        const val FLAG_CONTAINS_HEADER = 1 shl 31
    }
}
