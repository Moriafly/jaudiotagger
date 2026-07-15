package org.jaudiotagger.audio.ape

import org.jaudiotagger.audio.generic.AudioFileReader2
import org.jaudiotagger.audio.generic.GenericAudioHeader
import org.jaudiotagger.tag.Tag
import java.io.IOException
import java.nio.file.Path

class ApeFileReader : AudioFileReader2() {
    private val ir = ApeInfoReader()
    private val tr = ApeTagReader()

    override fun getEncodingInfo(path: Path): GenericAudioHeader = ir.read(path)

    override fun getTag(path: Path): Tag = tr.read(path)
}
