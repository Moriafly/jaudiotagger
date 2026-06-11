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
            put(FieldKey.LYRICS, ApeFieldKey.LYRICS)
            put(FieldKey.RATING, ApeFieldKey.RATING)
            put(FieldKey.ENCODER, ApeFieldKey.ENCODER)
            put(FieldKey.DISC_NO, ApeFieldKey.DISC)
            put(FieldKey.DISC_SUBTITLE, ApeFieldKey.DISC_SUBTITLE)
            put(FieldKey.REMIXER, ApeFieldKey.REMIXER)
            put(FieldKey.COVER_ART, ApeFieldKey.COVER_ART_FRONT)
        }
    }

    override fun isAllowedEncoding(enc: Charset): Boolean = enc == StandardCharsets.UTF_8

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

    override fun getFirstField(genericKey: FieldKey): TagField? {
        val apeFieldKey = tagFieldToApeField[genericKey] ?: return null
        return getFirstField(apeFieldKey.fieldName)
    }

    override fun deleteField(genericKey: FieldKey) {
        val apeFieldKey = tagFieldToApeField[genericKey]
            ?: throw KeyNotFoundException("No APE field key for $genericKey")
        deleteField(apeFieldKey.fieldName)
    }

    override fun getFields(genericKey: FieldKey): List<TagField> {
        val apeFieldKey = tagFieldToApeField[genericKey]
            ?: throw KeyNotFoundException("No APE field key for $genericKey")
        return super.getFields(apeFieldKey.fieldName)
    }

    override fun getAll(genericKey: FieldKey): List<String> {
        val apeFieldKey = tagFieldToApeField[genericKey]
            ?: throw KeyNotFoundException("No APE field key for $genericKey")
        return super.getAll(apeFieldKey.fieldName)
    }

    override fun getValue(genericKey: FieldKey, n: Int): String {
        val apeFieldKey = tagFieldToApeField[genericKey]
            ?: throw KeyNotFoundException("No APE field key for $genericKey")
        return getItem(apeFieldKey.fieldName, n)
    }

    override fun hasField(genericKey: FieldKey): Boolean {
        val apeFieldKey = tagFieldToApeField[genericKey] ?: return false
        return getFields(apeFieldKey.fieldName).isNotEmpty()
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
