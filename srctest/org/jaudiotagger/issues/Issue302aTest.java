package org.jaudiotagger.issues;

import org.jaudiotagger.AbstractTestCase;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import java.io.File;

import static org.jaudiotagger.tag.FieldKey.ALBUM;

public class Issue302aTest extends AbstractTestCase
{
    /**
     * Test Read Mp3
     */
    public void testReadMp3()
    {
        File orig = new File("testdata", "test302.mp3");
        if (!orig.isFile())
        {
            return;
        }

        Exception exceptionCaught = null;
        try
        {
            File testFile = AbstractTestCase.copyAudioToTmp("test302.mp3");
            AudioFile af = AudioFileIO.read(testFile);
            af.getTag().setField(ALBUM,"012345678901");
            af.commit();

            af = AudioFileIO.read(testFile);
            assertEquals(af.getTag().getFirst(FieldKey.ALBUM), "012345678901");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            exceptionCaught = e;
        }
        assertNull(exceptionCaught);
    }
}
