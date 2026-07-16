package org.jaudiotagger.audio.aac

/**
 * Parsed fixed and variable portions of an ADTS frame header.
 */
class AdtsHeader private constructor(
    val mpegVersion: Int,
    val profile: Int,
    val sampleRate: Int,
    val channelConfiguration: Int,
    val frameLength: Int,
    val bufferFullness: Int,
    val rawDataBlockCount: Int,
    private val protectionAbsent: Boolean
) {
    val profileName: String
        get() = PROFILE_NAMES[profile]

    val channelCount: Int
        get() = CHANNEL_COUNTS[channelConfiguration]

    val headerLength: Int
        get() = if (protectionAbsent) MIN_HEADER_LENGTH else CRC_HEADER_LENGTH

    val isVariableBitRate: Boolean
        get() = bufferFullness == VARIABLE_BIT_RATE_BUFFER_FULLNESS

    fun hasSameAudioConfiguration(other: AdtsHeader?): Boolean =
        other != null &&
            mpegVersion == other.mpegVersion &&
            profile == other.profile &&
            sampleRate == other.sampleRate &&
            channelConfiguration == other.channelConfiguration

    companion object {
        const val MIN_HEADER_LENGTH = 7
        const val CRC_HEADER_LENGTH = 9
        const val VARIABLE_BIT_RATE_BUFFER_FULLNESS = 0x7FF
        const val SAMPLES_PER_RAW_DATA_BLOCK = 1024

        private val SAMPLE_RATES = intArrayOf(
            96000,
            88200,
            64000,
            48000,
            44100,
            32000,
            24000,
            22050,
            16000,
            12000,
            11025,
            8000,
            7350
        )

        private val CHANNEL_COUNTS = intArrayOf(0, 1, 2, 3, 4, 5, 6, 8)
        private val PROFILE_NAMES = arrayOf("AAC Main", "AAC LC", "AAC SSR", "AAC LTP")

        /**
         * Parses an ADTS header from the first seven bytes of a frame.
         *
         * @return a parsed header, or `null` when the bytes are not a valid ADTS header
         */
        @JvmStatic
        fun parse(data: ByteArray?): AdtsHeader? {
            if (data == null || data.size < MIN_HEADER_LENGTH) {
                return null
            }

            val byte0 = data[0].toInt() and 0xFF
            val byte1 = data[1].toInt() and 0xFF
            val byte2 = data[2].toInt() and 0xFF
            val byte3 = data[3].toInt() and 0xFF
            val byte4 = data[4].toInt() and 0xFF
            val byte5 = data[5].toInt() and 0xFF
            val byte6 = data[6].toInt() and 0xFF

            // Twelve sync bits followed by a two-bit layer field that must be zero.
            if (byte0 != 0xFF || (byte1 and 0xF6) != 0xF0) {
                return null
            }

            val sampleRateIndex = (byte2 ushr 2) and 0x0F
            if (sampleRateIndex >= SAMPLE_RATES.size) {
                return null
            }

            val protectionAbsent = (byte1 and 0x01) != 0
            val headerLength = if (protectionAbsent) MIN_HEADER_LENGTH else CRC_HEADER_LENGTH
            val frameLength =
                ((byte3 and 0x03) shl 11) or (byte4 shl 3) or ((byte5 ushr 5) and 0x07)
            if (frameLength < headerLength) {
                return null
            }

            val channelConfiguration = ((byte2 and 0x01) shl 2) or ((byte3 ushr 6) and 0x03)
            return AdtsHeader(
                mpegVersion = (byte1 ushr 3) and 0x01,
                profile = (byte2 ushr 6) and 0x03,
                sampleRate = SAMPLE_RATES[sampleRateIndex],
                channelConfiguration = channelConfiguration,
                frameLength = frameLength,
                bufferFullness = ((byte5 and 0x1F) shl 6) or ((byte6 ushr 2) and 0x3F),
                rawDataBlockCount = (byte6 and 0x03) + 1,
                protectionAbsent = protectionAbsent
            )
        }
    }
}
