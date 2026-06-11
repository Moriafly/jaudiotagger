package org.jaudiotagger.tag.ape

enum class ApeFieldKey(
    val fieldName: String,
    val isBinary: Boolean,
    val isCommon: Boolean
) {
    TITLE("Title", false, true),
    SUBTITLE("Subtitle", false, false),
    ARTIST("Artist", false, true),
    ALBUM("Album", false, true),
    DEBUT_ALBUM("Debut Album", false, false),
    PUBLISHER("Publisher", false, false),
    CONDUCTOR("Conductor", false, false),
    TRACK("Track", false, true),
    COMPOSER("Composer", false, false),
    COMMENT("Comment", false, true),
    COPYRIGHT("Copyright", false, false),
    PUBLICATIONRIGHT("Publicationright", false, false),
    EAN_UPC("EAN/UPC", false, false),
    ISBN("ISBN", false, false),
    CATALOG("Catalog", false, false),
    LABEL("Label", false, false),
    LC("LC", false, false),
    YEAR("Year", false, true),
    RECORD_DATE("Record Date", false, false),
    RECORD_LOCATION("Record Location", false, false),
    GENRE("Genre", false, true),
    MEDIA("Media", false, false),
    ISRC("ISRC", false, false),
    ABSTRACT("Abstract", false, false),
    BIBLIOGRAPHY("Bibliography", false, false),
    LANGUAGE("Language", false, false),
    BPM("BPM", false, false),
    LYRICS("Lyrics", false, false),
    RATING("Rating", false, false),
    ENCODER("Encoder", false, false),
    DISC("Disc", false, false),
    ALBUM_ARTIST("Album Artist", false, false),
    DISC_SUBTITLE("Disc Subtitle", false, false),
    REMIXER("Remixer", false, false),
    COVER_ART_FRONT("Cover Art (Front)", true, false),
    COVER_ART_BACK("Cover Art (Back)", true, false),
    ;

    companion object {
        private val fieldNameMap: Map<String, ApeFieldKey> = entries.associateBy { it.fieldName }

        @JvmStatic
        fun getByName(fieldName: String): ApeFieldKey? = fieldNameMap[fieldName]
    }
}
