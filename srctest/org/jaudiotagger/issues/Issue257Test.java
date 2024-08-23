package org.jaudiotagger.issues;

import org.jaudiotagger.AbstractTestCase;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;

import java.io.File;

/**
 * Test Writing to new urls with common interface
 */
public class Issue257Test extends AbstractTestCase
{
    /**
     * Test Mp4 with crap between free atom and mdat atom, shoud cause immediate failure
     */
    public void testReadMp4FileWithPaddingAfterLastAtom()
    {
        File orig = new File("testdata", "test37.m4a");
        if (!orig.isFile())
        {
            System.err.println("Unable to test file - not available");
            return;
        }

        File testFile = null;
        Exception exceptionCaught = null;
        try
        {
            testFile = AbstractTestCase.copyAudioToTmp("test37.m4a");

            AudioFileIO.read(testFile);

            //Print Out Tree

        }
        catch(Exception e)
        {
            e.printStackTrace();
            exceptionCaught=e;
        }

        assertTrue(exceptionCaught instanceof CannotReadException);
    }
}
