package com.moriafly.jaudiotagger

import org.jaudiotagger.audio.AudioFileIO
import java.io.File
import kotlin.test.Test

class DffTest {
    @Test
    fun testDff() {
        val file = File("C:\\Users\\moria\\Videos\\王若琳 - Let's Start From Here.dff")
        println("Read DFF start")
        val audioFile = AudioFileIO.read(file)
        println("Read DFF end")
        println(audioFile.audioHeader)
    }
}
