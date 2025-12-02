@file:Suppress("ktlint:standard:function-naming")

package com.moriafly.jaudiotagger

import org.jaudiotagger.audio.AudioFileIO
import java.io.File
import kotlin.test.Test

class M4ATest {
    @Test
    fun `m4a bitrate`() {
        val file = File("C:\\Users\\Moriafly\\Music\\编码测试\\M4A\\Nakiso - Retey Now.m4a")
        val audioFile = AudioFileIO.read(file)
        val bitrate = audioFile.audioHeader.bitRate
        println("bitrate = $bitrate")
    }
}
