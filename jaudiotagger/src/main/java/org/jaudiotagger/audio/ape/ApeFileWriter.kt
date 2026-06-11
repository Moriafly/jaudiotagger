package org.jaudiotagger.audio.ape

import org.jaudiotagger.audio.exceptions.CannotReadException
import org.jaudiotagger.audio.exceptions.CannotWriteException
import org.jaudiotagger.audio.generic.AudioFileWriter2
import org.jaudiotagger.tag.Tag
import java.nio.file.Path

class ApeFileWriter : AudioFileWriter2() {
    private val tw = ApeTagWriter()

    override fun writeTag(tag: Tag, file: Path) {
        tw.write(tag, file)
    }

    override fun deleteTag(tag: Tag, file: Path) {
        tw.delete(tag, file)
    }
}
