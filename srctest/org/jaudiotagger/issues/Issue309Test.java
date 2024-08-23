package org.jaudiotagger.issues;

import org.jaudiotagger.AbstractTestCase;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;

import java.io.File;

/**
 * Reading mp4 with corrupt length recorded in tag ending up in middle of free atom should fail
 */
public class Issue309Test extends AbstractTestCase
{
    public static int countExceptions =0;

    public void testAddingLargeImageToOgg() throws Exception
    {
        File orig = new File("testdata", "test73.m4a");
        if (!orig.isFile())
        {
            System.err.println("Unable to test file - not available");
            return;
        }

        Exception e=null;
        try
        {
            final File testFile = AbstractTestCase.copyAudioToTmp("test73.m4a");
            AudioFileIO.read(testFile);

        }
        catch(Exception ex)
        {
            e=ex;
            ex.printStackTrace();
        }
        assertNotNull(e);
    }

    public void testM4a() throws Exception
    {
        File orig = new File("testdata", "test309.m4a");
        if (!orig.isFile())
        {
            System.err.println("Unable to test file - not available");
            return;
        }


        Exception e=null;
        try
        {
            final File testFile = AbstractTestCase.copyAudioToTmp("test309.m4a");
            AudioFile af = AudioFileIO.read(testFile);
            af.getTag().setField(FieldKey.MUSICBRAINZ_WORK,"fkmgfkjkfjbvkfdjvbkfnbfn");
            af.commit();
        }
        catch(Exception ex)
        {
            e=ex;
            ex.printStackTrace();
        }
        assertNull(e);
    }
}