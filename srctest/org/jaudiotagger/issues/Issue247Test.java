package org.jaudiotagger.issues;

import org.jaudiotagger.AbstractTestCase;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.mp4.Mp4AtomTree;
import org.jaudiotagger.tag.FieldKey;
import java.io.File;

/**
 * Test Writing to new urls with common interface
 */
public class Issue247Test extends AbstractTestCase
{
    /**
     * Test Write Mp4
     */
    public void testWriteMp4()
    {
        File orig = new File("testdata", "test247.m4a");
        if (!orig.isFile())
        {
            return;
        }

        Exception exceptionCaught = null;
        try
        {
            File testFile = AbstractTestCase.copyAudioToTmp("test247.m4a");

            new Mp4AtomTree(testFile).printAtomTree();

            AudioFile af = AudioFileIO.read(testFile);
            af.getTag().setField(FieldKey.ALBUM,"012345678901");
            af.commit();

            af = AudioFileIO.read(testFile);
            new Mp4AtomTree(testFile).printAtomTree();

        }
        catch (Exception e)
        {
            e.printStackTrace();
            exceptionCaught = e;
        }
        assertNull(exceptionCaught);
    }
}
