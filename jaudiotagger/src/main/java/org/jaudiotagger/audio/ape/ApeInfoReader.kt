package org.jaudiotagger.audio.ape

import org.jaudiotagger.audio.exceptions.CannotReadException
import org.jaudiotagger.audio.generic.GenericAudioHeader
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path

class ApeInfoReader {

    @Throws(CannotReadException::class, IOException::class)
    fun read(path: Path): GenericAudioHeader {
        val fileLength = Files.size(path)
        if (fileLength < MINIMUM_FILE_SIZE) {
            throw CannotReadException("File too small to be a valid APE file")
        }

        val channel = Files.newByteChannel(path)
        channel.use {
            val buf = ByteBuffer.allocate(76).order(ByteOrder.LITTLE_ENDIAN)
            channel.read(buf)
            buf.flip()

            val magic = ByteArray(4)
            buf.get(magic)
            if (magic.decodeToString() != MAC_MAGIC) {
                throw CannotReadException("Not a valid APE file: missing MAC magic")
            }

            val version = buf.short.toInt() and 0xFFFF
            val audioHeader = GenericAudioHeader()
            audioHeader.encodingType = "Monkey's Audio"
            audioHeader.setLossless(true)

            if (version >= 3980) {
                readNewFormat(channel, audioHeader)
            } else {
                readOldFormat(buf, version, audioHeader, fileLength)
            }

            return audioHeader
        }
    }

    private fun readNewFormat(channel: java.nio.channels.SeekableByteChannel, audioHeader: GenericAudioHeader) {
        val descBuf = ByteBuffer.allocate(52).order(ByteOrder.LITTLE_ENDIAN)
        channel.position(0)
        channel.read(descBuf)
        descBuf.flip()

        descBuf.position(6)
        descBuf.short // padding
        val descriptorLength = descBuf.int and 0xFFFFFFF
        val headerLength = descBuf.int and 0xFFFFFFF
        val seekTableLength = descBuf.int and 0xFFFFFFF
        val wavHeaderLength = descBuf.int and 0xFFFFFFF
        val audioDataLengthLow = descBuf.int and 0xFFFFFFF
        val audioDataLengthHigh = descBuf.int and 0xFFFFFFF
        val wavTailLength = descBuf.int and 0xFFFFFFF

        val headerBuf = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
        channel.position(descriptorLength.toLong())
        channel.read(headerBuf)
        headerBuf.flip()

        val compressionLevel = headerBuf.short.toInt() and 0xFFFF
        val formatFlags = headerBuf.short.toInt() and 0xFFFF
        val blocksPerFrame = headerBuf.int and 0xFFFFFFF
        val finalFrameBlocks = headerBuf.int and 0xFFFFFFF
        val totalFrames = headerBuf.int and 0xFFFFFFF
        val bitsPerSample = headerBuf.short.toInt() and 0xFFFF
        val channels = headerBuf.short.toInt() and 0xFFFF
        val sampleRate = headerBuf.int and 0xFFFFFFF

        val totalBlocks = (totalFrames - 1L) * blocksPerFrame + finalFrameBlocks
        val duration = if (sampleRate > 0) totalBlocks.toDouble() / sampleRate else 0.0

        val audioDataLength = (audioDataLengthHigh.toLong() shl 32) or (audioDataLengthLow.toLong() and 0xFFFFFFFFL)

        audioHeader.setPreciseLength(duration)
        audioHeader.setChannelNumber(channels)
        audioHeader.setSamplingRate(sampleRate)
        audioHeader.setBitsPerSample(bitsPerSample)
        audioHeader.setBitRate(calculateBitrateFromDataSize(audioDataLength, duration))
    }

    private fun readOldFormat(buf: ByteBuffer, version: Int, audioHeader: GenericAudioHeader, fileLength: Long) {
        buf.position(6)
        val compressionLevel = buf.short.toInt() and 0xFFFF
        val formatFlags = buf.short.toInt() and 0xFFFF
        val channels = buf.short.toInt() and 0xFFFF
        val sampleRate = buf.int and 0xFFFFFFF
        val wavHeaderLength = buf.int and 0xFFFFFFF
        val wavTailLength = buf.int and 0xFFFFFFF
        val totalFrames = buf.int and 0xFFFFFFF
        val finalFrameBlocks = buf.int and 0xFFFFFFF

        val bitsPerSample = if (formatFlags and FORMAT_FLAG_24_BIT != 0) 24 else 16

        val blocksPerFrame = if (version >= 3950) 73728 * 4
            else if (version >= 3900 || (version >= 3800 && compressionLevel >= 4000)) 73728
            else 9216

        val totalBlocks = (totalFrames - 1L) * blocksPerFrame + finalFrameBlocks
        val duration = if (sampleRate > 0) totalBlocks.toDouble() / sampleRate else 0.0

        // For old format, estimate audio data size from file structure
        var headerSize = 32 // basic header
        if (formatFlags and FORMAT_FLAG_HAS_PEAK_LEVEL != 0) headerSize += 4
        if (formatFlags and FORMAT_FLAG_HAS_SEEK_ELEMENTS != 0) headerSize += 4
        if (formatFlags and FORMAT_FLAG_CREATE_WAV_HEADER == 0) headerSize += wavHeaderLength
        val seekTableSize = totalFrames * 4
        val audioDataLength = fileLength - headerSize - seekTableSize - wavTailLength

        audioHeader.setPreciseLength(duration)
        audioHeader.setChannelNumber(channels)
        audioHeader.setSamplingRate(sampleRate)
        audioHeader.setBitsPerSample(bitsPerSample)
        audioHeader.setBitRate(calculateBitrateFromDataSize(audioDataLength, duration))
    }

    private fun calculateBitrateFromDataSize(dataLength: Long, duration: Double): Int {
        if (duration <= 0 || dataLength <= 0) return 0
        return ((dataLength * 8) / (duration * 1000)).toInt()
    }

    companion object {
        private const val MAC_MAGIC = "MAC "
        private const val MINIMUM_FILE_SIZE = 64L
        private const val FORMAT_FLAG_24_BIT = 8
        private const val FORMAT_FLAG_HAS_PEAK_LEVEL = 4
        private const val FORMAT_FLAG_HAS_SEEK_ELEMENTS = 16
        private const val FORMAT_FLAG_CREATE_WAV_HEADER = 32
    }
}
