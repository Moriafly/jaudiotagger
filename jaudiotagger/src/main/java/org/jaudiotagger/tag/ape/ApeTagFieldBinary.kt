package org.jaudiotagger.tag.ape

import org.jaudiotagger.tag.TagField
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

class ApeTagFieldBinary(
    private val id: String,
    private val description: String = "",
    private var binaryData: ByteArray = ByteArray(0)
) : TagField {

    override fun getId(): String = id

    override fun isBinary(): Boolean = true

    override fun isBinary(b: Boolean) {
        // Accept any value, always binary
    }

    override fun isCommon(): Boolean = false

    override fun isEmpty(): Boolean = binaryData.isEmpty()

    override fun copyContent(field: TagField) {
        if (field is ApeTagFieldBinary) {
            binaryData = field.binaryData.copyOf()
        }
    }

    fun getBinaryData(): ByteArray = binaryData

    fun setBinaryData(data: ByteArray) {
        binaryData = data
    }

    fun getDescription(): String = description

    override fun getRawContent(): ByteArray {
        val keyBytes = id.toByteArray(StandardCharsets.US_ASCII)
        val descriptionBytes = description.toByteArray(StandardCharsets.UTF_8)
        val valueSize = descriptionBytes.size + 1 + binaryData.size
        val flags = ITEM_FLAG_BINARY

        val buffer = ByteBuffer.allocate(4 + 4 + keyBytes.size + 1 + valueSize)
        buffer.putInt(valueSize)
        buffer.putInt(flags)
        buffer.put(keyBytes)
        buffer.put(0x00)
        buffer.put(descriptionBytes)
        buffer.put(0x00)
        buffer.put(binaryData)
        return buffer.array()
    }

    override fun toString(): String = "[binary ${binaryData.size} bytes]"

    companion object {
        const val ITEM_FLAG_BINARY = 0x00000002
    }
}
