package org.jaudiotagger.issues;

import org.jaudiotagger.AbstractTestCase;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.mp4.Mp4AtomTree;
import java.io.File;

/**
 * Test Writing to new urls with common interface
 */
public class Issue277aTest extends AbstractTestCase
{
    /**
     * Test Write Mp4
     */
    public void testWriteMp4()
    {
        File orig = new File("testdata", "test277.m4a");
        if (!orig.isFile())
        {
            return;
        }

        try
        {
            File testFile = AbstractTestCase.copyAudioToTmp("test277.m4a");

            new Mp4AtomTree(testFile).printAtomTree();

            AudioFileIO.read(testFile);

            //af = AudioFileIO.read(testFile);
            //new Mp4AtomTree(testFile).printAtomTree();

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        //assertNull(exceptionCaught);
    }
}
