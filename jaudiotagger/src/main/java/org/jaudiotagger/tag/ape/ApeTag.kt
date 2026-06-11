package org.jaudiotagger.tag.ape

import org.jaudiotagger.audio.generic.AbstractTag
import org.jaudiotagger.tag.*
import org.jaudiotagger.tag.images.Artwork
import org.jaudiotagger.tag.images.ArtworkFactory
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.EnumMap

class ApeTag : AbstractTag() {

    companion object {
        private val tagFieldToApeField = EnumMap<FieldKey, ApeFieldKey>(FieldKey::class.java).apply {
            put(FieldKey.TITLE, ApeFieldKey.TITLE)
            put(FieldKey.SUBTITLE, ApeFieldKey.SUBTITLE)
            put(FieldKey.ARTIST, ApeFieldKey.ARTIST)
            put(FieldKey.ALBUM, ApeFieldKey.ALBUM)
            put(FieldKey.ALBUM_ARTIST, ApeFieldKey.ALBUM_ARTIST)
            put(FieldKey.COMPOSER, ApeFieldKey.COMPOSER)
            put(FieldKey.CONDUCTOR, ApeFieldKey.CONDUCTOR)
            put(FieldKey.TRACK, ApeFieldKey.TRACK)
            put(FieldKey.TRACK_TOTAL, ApeFieldKey.TRACK)
            put(FieldKey.YEAR, ApeFieldKey.YEAR)
            put(FieldKey.GENRE, ApeFieldKey.GENRE)
            put(FieldKey.COMMENT, ApeFieldKey.COMMENT)
            put(FieldKey.COPYRIGHT, ApeFieldKey.COPYRIGHT)
            put(FieldKey.RECORD_LABEL, ApeFieldKey.LABEL)
            put(FieldKey.ISRC, ApeFieldKey.ISRC)
            put(FieldKey.BPM, ApeFieldKey.BPM)
            put(FieldKey.LYRICS, ApeFieldKey.UNSYNCEDLYRICS)
            put(FieldKey.RATING, ApeFieldKey.RATING)
            put(FieldKey.ENCODER, ApeFieldKey.ENCODER)
            put(FieldKey.DISC_NO, ApeFieldKey.DISC)
            put(FieldKey.DISC_SUBTITLE, ApeFieldKey.DISC_SUBTITLE)
            put(FieldKey.REMIXER, ApeFieldKey.REMIXER)
            put(FieldKey.COVER_ART, ApeFieldKey.COVER_ART_FRONT)
        }

        // Fields that have fallback keys
        private val fallbackKeys = mapOf(
            FieldKey.LYRICS to listOf(ApeFieldKey.LYRICS, ApeFieldKey.UNSYNCEDLYRICS)
        )
    }

    override fun isAllowedEncoding(enc: Charset): Boolean = enc == StandardCharsets.UTF_8

    override fun addField(field: TagField) {
        val normalizedField = normalizeField(field)
        super.addField(normalizedField)
    }

    override fun setField(field: TagField) {
        val normalizedField = normalizeField(field)
        super.setField(normalizedField)
    }

    private fun normalizeField(field: TagField): TagField {
        if (field is ApeTagField) {
            val upperId = field.id.uppercase()
            if (upperId != field.id) {
                return ApeTagField(upperId, field.content)
            }
        } else if (field is ApeTagFieldBinary) {
            val upperId = field.id.uppercase()
            if (upperId != field.id) {
                return ApeTagFieldBinary(upperId, field.getDescription(), field.getBinaryData())
            }
        }
        return field
    }

    override fun getFields(id: String): List<TagField> = super.getFields(id.uppercase())

    override fun getFirst(id: String): String = super.getFirst(id.uppercase())

    override fun getFirstField(id: String): TagField? = super.getFirstField(id.uppercase())

    override fun deleteField(key: String) {
        super.deleteField(key.uppercase())
    }

    override fun hasField(id: String): Boolean = super.hasField(id.uppercase())

    override fun createField(genericKey: FieldKey, vararg value: String): TagField {
        val apeFieldKey = tagFieldToApeField[genericKey]
            ?: throw KeyNotFoundException("No APE field key for $genericKey")

        if (apeFieldKey.isBinary) {
            throw UnsupportedOperationException("Use createField(Artwork) for binary fields")
        }

        return ApeTagField(apeFieldKey.fieldName, value[0])
    }

    fun createField(apeFieldKey: ApeFieldKey, value: String): TagField {
        if (apeFieldKey.isBinary) {
            throw UnsupportedOperationException("Use createField(Artwork) for binary fields")
        }
        return ApeTagField(apeFieldKey.fieldName, value)
    }

    private fun getApeFieldNames(genericKey: FieldKey): List<String> {
        // Check if there are fallback keys
        fallbackKeys[genericKey]?.let { fallbacks ->
            return fallbacks.map { it.fieldName }
        }
        // Default: single mapping
        val apeFieldKey = tagFieldToApeField[genericKey] ?: return emptyList()
        return listOf(apeFieldKey.fieldName)
    }

    override fun getFirstField(genericKey: FieldKey): TagField? {
        val fieldNames = getApeFieldNames(genericKey)
        for (name in fieldNames) {
            val field = getFirstField(name)
            if (field != null) return field
        }
        return null
    }

    override fun deleteField(genericKey: FieldKey) {
        val fieldNames = getApeFieldNames(genericKey)
        if (fieldNames.isEmpty()) {
            throw KeyNotFoundException("No APE field key for $genericKey")
        }
        for (name in fieldNames) {
            deleteField(name)
        }
    }

    override fun getFields(genericKey: FieldKey): List<TagField> {
        val fieldNames = getApeFieldNames(genericKey)
        for (name in fieldNames) {
            val fields = super.getFields(name)
            if (fields.isNotEmpty()) return fields
        }
        return emptyList()
    }

    override fun getAll(genericKey: FieldKey): List<String> {
        val fieldNames = getApeFieldNames(genericKey)
        for (name in fieldNames) {
            val values = super.getAll(name)
            if (values.isNotEmpty()) return values
        }
        return emptyList()
    }

    override fun getValue(genericKey: FieldKey, n: Int): String {
        val fieldNames = getApeFieldNames(genericKey)
        for (name in fieldNames) {
            val value = getItem(name, n)
            if (value.isNotEmpty()) return value
        }
        return ""
    }

    override fun hasField(genericKey: FieldKey): Boolean {
        val fieldNames = getApeFieldNames(genericKey)
        for (name in fieldNames) {
            if (super.getFields(name).isNotEmpty()) return true
        }
        return false
    }

    override fun getArtworkList(): List<Artwork> {
        val artworkList = mutableListOf<Artwork>()

        for (key in listOf(ApeFieldKey.COVER_ART_FRONT, ApeFieldKey.COVER_ART_BACK)) {
            val fieldList = getFields(key.fieldName)
            for (field in fieldList) {
                if (field is ApeTagFieldBinary) {
                    val artwork = ArtworkFactory.getNew()
                    artwork.setBinaryData(field.getBinaryData())
                    artwork.setDescription(field.getDescription())
                    artwork.setMimeType(guessMimeType(field.getBinaryData()))
                    artworkList.add(artwork)
                }
            }
        }

        return artworkList
    }

    override fun createField(artwork: Artwork): TagField {
        val binaryData = if (artwork.isLinked) {
            artwork.imageUrl.toByteArray(StandardCharsets.UTF_8)
        } else {
            if (!artwork.setImageFromData()) {
                throw FieldDataInvalidException("Unable to read artwork data")
            }
            artwork.binaryData
        }

        val key = if (artwork.pictureType == 3) {
            ApeFieldKey.COVER_ART_FRONT.fieldName
        } else {
            ApeFieldKey.COVER_ART_BACK.fieldName
        }

        return ApeTagFieldBinary(key, artwork.description ?: "", binaryData)
    }

    override fun deleteArtworkField() {
        deleteField(ApeFieldKey.COVER_ART_FRONT.fieldName)
        deleteField(ApeFieldKey.COVER_ART_BACK.fieldName)
    }

    override fun createCompilationField(value: Boolean): TagField {
        return createField(FieldKey.IS_COMPILATION, value.toString())
    }

    private fun guessMimeType(data: ByteArray): String {
        if (data.size >= 3 && data[0] == 0xFF.toByte() && data[1] == 0xD8.toByte() && data[2] == 0xFF.toByte()) {
            return "image/jpeg"
        }
        if (data.size >= 8 && data[0] == 0x89.toByte() && data[1] == 0x50.toByte() &&
            data[2] == 0x4E.toByte() && data[3] == 0x47.toByte()
        ) {
            return "image/png"
        }
        if (data.size >= 4 && data[0] == 0x47.toByte() && data[1] == 0x49.toByte() &&
            data[2] == 0x46.toByte() && data[3] == 0x38.toByte()
        ) {
            return "image/gif"
        }
        return "image/unknown"
    }

    override fun toString(): String = "APEv2 ${super.toString()}"
}
