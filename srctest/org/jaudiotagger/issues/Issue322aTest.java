package org.jaudiotagger.issues;

import org.jaudiotagger.AbstractTestCase;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagOptionSingleton;

import java.io.File;

/**
 * Test, useful for just reading file info
 */
public class Issue322aTest extends AbstractTestCase
{
    public void testReadFile() throws Exception
    {
        final TagOptionSingleton tagOptions = TagOptionSingleton.getInstance();
        tagOptions.setToDefault();

        File orig = new File("testdata", "test322.dsf");
        if (!orig.isFile())
        {
            System.err.println("Unable to test file - not available");
            return;
        }

        Exception ex=null;
        try
        {
            File testFile = AbstractTestCase.copyAudioToTmp("test322.dsf");
            AudioFile af = AudioFileIO.read(testFile);
            assertNotNull(af.getTag());
            System.out.println(af.getTag());
        }
        catch(Exception e)
        {
            e.printStackTrace();
            ex=e;
        }
        assertNull(ex);
    }

    public void testWriteFile() throws Exception
    {
        final TagOptionSingleton tagOptions = TagOptionSingleton.getInstance();
        tagOptions.setToDefault();

        File orig = new File("testdata", "test322.dsf");
        if (!orig.isFile())
        {
            System.err.println("Unable to test file - not available");
            return;
        }

        Exception ex=null;
        try
        {
            File testFile = AbstractTestCase.copyAudioToTmp("test322.dsf");
            AudioFile af = AudioFileIO.read(testFile);
            assertNotNull(af.getTag());
            af.getTag().setField(FieldKey.ARTIST,"Freddy");
            af.commit();
            af = AudioFileIO.read(testFile);
            assertNotNull(af.getTag());
            assertEquals(af.getTag().getFirst(FieldKey.ARTIST),"Freddy");
            System.out.println(af.getTag());
        }
        catch(Exception e)
        {
            e.printStackTrace();
            ex=e;
        }
        assertNull(ex);
    }
}
