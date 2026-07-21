package org.jaudiotagger.audio.mp4;

/**
 * Encoder Type actually identifies the format of the audio within the mp4. This is because
 * mp4 container can be used to hold different types of files.
 */
public enum EncoderType {
    AAC("AAC"),
    DRM_AAC("AAC (DRM)"),
    APPLE_LOSSLESS("ALAC"),
    DOLBY_DIGITAL_PLUS("E-AC-3"),
    DOLBY_AC_4("AC-4"),
    ;

    private String description;

    EncoderType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
