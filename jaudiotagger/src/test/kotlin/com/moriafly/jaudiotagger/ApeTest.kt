package com.moriafly.jaudiotagger

import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.ape.ApeTagField
import org.jaudiotagger.tag.ape.ApeTagFieldBinary
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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

        println("\n=== Tag via FieldKey ===")
        val tag = audioFile.tag
        println("Tag type: ${tag.javaClass.simpleName}")
        println("Field count: ${tag.fieldCount}")

        val title = tag.getFirst(FieldKey.TITLE)
        val artist = tag.getFirst(FieldKey.ARTIST)
        val album = tag.getFirst(FieldKey.ALBUM)
        val lyrics = tag.getFirst(FieldKey.LYRICS)

        println("TITLE: $title")
        println("ARTIST: $artist")
        println("ALBUM: $album")
        println("LYRICS (first 50 chars): ${lyrics.take(50)}...")

        assertNotNull(title, "TITLE should not be null")
        assertTrue(title.isNotEmpty(), "TITLE should not be empty")
        assertNotNull(artist, "ARTIST should not be null")
        assertTrue(artist.isNotEmpty(), "ARTIST should not be empty")
        assertNotNull(lyrics, "LYRICS should not be null")
        assertTrue(lyrics.isNotEmpty(), "LYRICS should not be empty")

        println("\n=== Artwork ===")
        val artworkList = tag.artworkList
        println("Artwork count: ${artworkList.size}")
        for (artwork in artworkList) {
            println("  MimeType: ${artwork.mimeType}, Size: ${artwork.binaryData?.size ?: 0} bytes")
        }
    }
}
