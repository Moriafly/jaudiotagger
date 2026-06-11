package com.moriafly.jaudiotagger

import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.ape.ApeTagField
import org.jaudiotagger.tag.ape.ApeTagFieldBinary
import java.io.File
import kotlin.test.Test

class ApeTest {
    @Test
    fun testApe() {
        val file = File(System.getProperty("user.home"), "Music/Beyond - 灰色的轨迹.ape")
        if (!file.exists()) {
            println("APE test file not found, skipping")
            return
        }
        println("Reading APE file: ${file.name}")
        val audioFile = AudioFileIO.read(file)

        println("\n=== Audio Header ===")
        println("Format: ${audioFile.audioHeader.encodingType}")
        println("Sample Rate: ${audioFile.audioHeader.sampleRate} Hz")
        println("Channels: ${audioFile.audioHeader.channels}")
        println("Bits Per Sample: ${audioFile.audioHeader.bitsPerSample}")
        println("Bitrate: ${audioFile.audioHeader.bitRate} kbps")
        println("Duration: ${audioFile.audioHeader.trackLength} seconds")
        println("Lossless: ${audioFile.audioHeader.isLossless}")

        println("\n=== Tag ===")
        val tag = audioFile.tag
        println("Tag type: ${tag.javaClass.simpleName}")
        println("Field count: ${tag.fieldCount}")

        val fields = tag.getFields()
        while (fields.hasNext()) {
            val field = fields.next()
            if (field is ApeTagFieldBinary) {
                println("  ${field.id}: [binary ${field.getBinaryData().size} bytes]")
            } else {
                println("  ${field.id}: ${field}")
            }
        }

        println("\n=== Artwork ===")
        val artworkList = tag.artworkList
        println("Artwork count: ${artworkList.size}")
        for (artwork in artworkList) {
            println("  MimeType: ${artwork.mimeType}, Size: ${artwork.binaryData?.size ?: 0} bytes")
        }
    }
}
