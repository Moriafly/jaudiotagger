package org.jaudiotagger.audio.aac

import org.jaudiotagger.audio.exceptions.CannotReadException
import org.jaudiotagger.audio.generic.AudioFileReader2
import org.jaudiotagger.audio.generic.GenericAudioHeader
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.TagException
import org.jaudiotagger.tag.id3.AbstractID3v2Tag
import org.jaudiotagger.tag.id3.ID3v22Tag
import org.jaudiotagger.tag.id3.ID3v23Tag
import org.jaudiotagger.tag.id3.ID3v24Tag
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.util.logging.Level
import kotlin.math.roundToInt

/**
 * Reads raw AAC streams framed with ADTS, optionally prefixed by an ID3v2 tag.
 */
class AacFileReader : AudioFileReader2() {
    @Throws(CannotReadException::class, IOException::class)
    override fun getEncodingInfo(path: Path): GenericAudioHeader {
        val fileSize = Files.size(path)
        val audioStart = getAudioStart(path)
        if (audioStart >= fileSize) {
            throw CannotReadException("$path does not contain ADTS audio after its ID3v2 tag")
        }

        var firstHeader: AdtsHeader? = null
        var frameCount = 0L
        var sampleCount = 0L
        var position = audioStart
        var minimumFrameLength = Int.MAX_VALUE
        var maximumFrameLength = 0
        var variableBitRate = false
        val headerBytes = ByteArray(AdtsHeader.MIN_HEADER_LENGTH)

        Files.newInputStream(path).buffered(INPUT_BUFFER_SIZE).use { input ->
            input.skipFully(audioStart)
            while (position < fileSize) {
                if (fileSize - position < AdtsHeader.MIN_HEADER_LENGTH) {
                    throw CannotReadException("$path has a truncated ADTS header at byte $position")
                }

                input.readFully(headerBytes)
                val header = AdtsHeader.parse(headerBytes)
                    ?: throw CannotReadException(
                        "$path has an invalid ADTS header at byte $position"
                    )
                if (position + header.frameLength > fileSize) {
                    throw CannotReadException("$path has a truncated ADTS frame at byte $position")
                }

                if (firstHeader == null) {
                    firstHeader = header
                    if (header.channelCount == 0) {
                        throw CannotReadException(
                            "$path uses an ADTS program configuration element, " +
                                "which is not supported"
                        )
                    }
                } else if (!firstHeader.hasSameAudioConfiguration(header)) {
                    throw CannotReadException(
                        "$path changes ADTS audio configuration at byte $position"
                    )
                }

                frameCount++
                sampleCount +=
                    header.rawDataBlockCount.toLong() * AdtsHeader.SAMPLES_PER_RAW_DATA_BLOCK
                minimumFrameLength = minOf(minimumFrameLength, header.frameLength)
                maximumFrameLength = maxOf(maximumFrameLength, header.frameLength)
                variableBitRate = variableBitRate || header.isVariableBitRate

                input.skipFully((header.frameLength - AdtsHeader.MIN_HEADER_LENGTH).toLong())
                position += header.frameLength
            }
        }

        val streamHeader = firstHeader
            ?: throw CannotReadException("$path does not contain any ADTS frames")
        val duration = sampleCount.toDouble() / streamHeader.sampleRate
        val audioDataLength = fileSize - audioStart
        val bitRate = (audioDataLength * 8.0 / duration / 1000.0).roundToInt()
        val byteRate = (audioDataLength / duration).roundToInt()

        return GenericAudioHeader().apply {
            format = "AAC"
            encodingType = streamHeader.profileName
            setSamplingRate(streamHeader.sampleRate)
            channelNumber = streamHeader.channelCount
            noOfSamples = sampleCount
            setPreciseLength(duration)
            setBitRate(bitRate)
            setByteRate(byteRate)
            // ADTS does not declare PCM bit depth; match the decoded-audio default used by MP3/M4A.
            setBitsPerSample(DEFAULT_DECODED_BITS_PER_SAMPLE)
            isVariableBitRate = variableBitRate || minimumFrameLength != maximumFrameLength
            isLossless = false
            audioDataStartPosition = audioStart
            audioDataEndPosition = fileSize
            setAudioDataLength(audioDataLength)
        }.also { audioHeader ->
            logger.log(
                Level.FINE,
                "Created ADTS AAC audio header from {0} frames: {1}",
                arrayOf(frameCount, audioHeader)
            )
        }
    }

    @Throws(CannotReadException::class, IOException::class)
    override fun getTag(path: Path): Tag {
        val tagSize = getAudioStart(path)
        if (tagSize == 0L) {
            return ID3v24Tag()
        }
        if (tagSize > Int.MAX_VALUE) {
            throw CannotReadException("$path has an ID3v2 tag that is too large to read")
        }

        val buffer = ByteBuffer.allocate(tagSize.toInt())
        FileChannel.open(path).use { channel ->
            while (buffer.hasRemaining()) {
                if (channel.read(buffer) < 0) {
                    throw EOFException("Unexpected end of ID3v2 tag")
                }
            }
        }
        buffer.flip()

        val version = buffer[AbstractID3v2Tag.FIELD_TAG_MAJOR_VERSION_POS].toInt() and 0xFF
        try {
            return when (version) {
                ID3v22Tag.MAJOR_VERSION.toInt() -> ID3v22Tag(buffer, path.toString())
                ID3v23Tag.MAJOR_VERSION.toInt() -> ID3v23Tag(buffer, path.toString())
                ID3v24Tag.MAJOR_VERSION.toInt() -> ID3v24Tag(buffer, path.toString())
                else -> throw CannotReadException("$path has an unsupported ID3v2 version $version")
            }
        } catch (exception: TagException) {
            throw CannotReadException("$path has an invalid ID3v2 tag", exception)
        }
    }

    @Throws(IOException::class)
    private fun getAudioStart(path: Path): Long =
        AbstractID3v2Tag.getV2TagSizeIfExists(path.toFile())

    @Throws(IOException::class)
    private fun InputStream.readFully(data: ByteArray) {
        var offset = 0
        while (offset < data.size) {
            val read = read(data, offset, data.size - offset)
            if (read < 0) {
                throw EOFException("Unexpected end of ADTS header")
            }
            offset += read
        }
    }

    @Throws(IOException::class)
    private fun InputStream.skipFully(length: Long) {
        var remaining = length
        while (remaining > 0) {
            val skipped = skip(remaining)
            when {
                skipped > 0 -> remaining -= skipped
                read() >= 0 -> remaining--
                else -> throw EOFException("Unexpected end of ADTS frame")
            }
        }
    }

    companion object {
        private const val INPUT_BUFFER_SIZE = 64 * 1024
        private const val DEFAULT_DECODED_BITS_PER_SAMPLE = 16
    }
}
