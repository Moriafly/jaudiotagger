package org.jaudiotagger.issues;

import org.jaudiotagger.AbstractTestCase;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.TagOptionSingleton;

import java.io.File;

/**
 * #316:NullPointerException at org.jaudiotagger.tag.mp4.field.Mp4GenreField
 */
public class Issue317Test extends AbstractTestCase
{
    public void testReadFile() throws Exception
    {
        final TagOptionSingleton tagOptions = TagOptionSingleton.getInstance();
        tagOptions.setToDefault();

        File orig = new File("testdata", "test317.flac");
        if (!orig.isFile())
        {
            System.err.println("Unable to test file - not available");
            return;
        }

        Exception ex=null;
        try
        {
            File testFile = AbstractTestCase.copyAudioToTmp("test317.flac");
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
}
