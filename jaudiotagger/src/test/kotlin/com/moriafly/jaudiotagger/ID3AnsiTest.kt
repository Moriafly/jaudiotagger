package com.moriafly.jaudiotagger

import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import kotlin.test.Test

class ID3AnsiTest {
    @UnstableJaudiotaggerApi
    @Test
    fun `mp3 gbk`() {
        JaudiotaggerFlags.id3v1DecodingCharset = charset("GBK")
        val file = File("C:\\Users\\moria\\Music\\编码测试\\MP3\\乱码\\01.左小诅咒-像孩子似的倾听.mp3")
        val audioFile = AudioFileIO.read(file)
        val tag = audioFile.tag

        val title = tag.getFirst(FieldKey.TITLE)
        println(title)

        AudioFileIO.write(audioFile)
    }
}
