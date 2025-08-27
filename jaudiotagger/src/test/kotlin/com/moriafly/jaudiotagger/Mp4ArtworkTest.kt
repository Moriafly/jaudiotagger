package com.moriafly.jaudiotagger

import org.jaudiotagger.audio.AudioFileIO
import java.io.File
import kotlin.system.measureTimeMillis
import kotlin.test.Test

class Mp4ArtworkTest {
    @Test
    fun `test M4A ALAC artwork`() {
        val file = File("C:\\Users\\moria\\Music\\编码测试\\M4A\\01.微茫.m4a")
        val ms =
            measureTimeMillis {
                val audioFile = AudioFileIO.read(file)
                val firstArtworkBinaryData = audioFile.tag.firstArtwork.binaryData
            }
        println("耗时：$ms ms")
    }
}
