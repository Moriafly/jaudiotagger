package org.jaudiotagger.issues;

import org.jaudiotagger.AbstractTestCase;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.wav.WavSaveOptions;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagOptionSingleton;

import java.io.File;

/**
 * reading and writing to file with invalid picture type
 */
public class Issue318Test extends AbstractTestCase
{
    public void testWriteFile() throws Exception
    {
        final TagOptionSingleton tagOptions = TagOptionSingleton.getInstance();
        tagOptions.setWavSaveOptions(WavSaveOptions.SAVE_ACTIVE);

        File orig = new File("testdata", "test540.flac");
        if (!orig.isFile())
        {
            System.err.println("Unable to test file - not available");
            return;
        }

        Exception ex=null;
        try
        {
            File testFile = AbstractTestCase.copyAudioToTmp("test540.flac");
            AudioFile af = AudioFileIO.read(testFile);
            assertNotNull(af.getTag());
            System.out.println(af.getTag());

            af.getTagOrCreateAndSetDefault().setField(FieldKey.ARTIST,"artist");
            af.commit();

            af = AudioFileIO.read(testFile);
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
