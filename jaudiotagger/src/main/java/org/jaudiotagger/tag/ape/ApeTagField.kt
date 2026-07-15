package org.jaudiotagger.tag.ape

import org.jaudiotagger.tag.TagField
import org.jaudiotagger.tag.TagTextField
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.Objects

class ApeTagField(
    private val id: String,
    private var content: String
) : TagTextField {

    override fun getId(): String = id

    override fun getContent(): String = content

    override fun setContent(content: String) {
        this.content = content
    }

    override fun getEncoding(): Charset = StandardCharsets.UTF_8

    override fun setEncoding(encoding: Charset) {
        // APE is always UTF-8, no-op
    }

    override fun isBinary(): Boolean = false

    override fun isBinary(b: Boolean) {
        if (b) throw UnsupportedOperationException("APE text fields cannot be set to binary")
    }

    override fun isCommon(): Boolean {
        val apeKey = ApeFieldKey.getByName(id)
        return apeKey?.isCommon ?: false
    }

    override fun isEmpty(): Boolean = content.isEmpty()

    override fun copyContent(field: TagField) {
        if (field is ApeTagField) {
            content = field.content
        }
    }

    override fun getRawContent(): ByteArray {
        val keyBytes = id.toByteArray(StandardCharsets.US_ASCII)
        val valueBytes = content.toByteArray(StandardCharsets.UTF_8)
        val valueSize = valueBytes.size
        val flags = ITEM_FLAG_TEXT

        val buffer = ByteBuffer.allocate(4 + 4 + keyBytes.size + 1 + valueSize)
        buffer.putInt(valueSize)
        buffer.putInt(flags)
        buffer.put(keyBytes)
        buffer.put(0x00)
        buffer.put(valueBytes)
        return buffer.array()
    }

    override fun toString(): String = content

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ApeTagField) return false
        return id == other.id && content == other.content
    }

    override fun hashCode(): Int = Objects.hash(id, content)

    companion object {
        const val ITEM_FLAG_TEXT = 0x00000000
    }
}
