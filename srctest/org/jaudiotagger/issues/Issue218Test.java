package org.jaudiotagger.issues;

import org.jaudiotagger.AbstractTestCase;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.mp4.Mp4AtomTree;
import org.jaudiotagger.tag.FieldKey;

import java.io.File;

/**
 * Test
 */
public class Issue218Test extends AbstractTestCase
{
    public void testReadCorruptMp4ThatLooksLikehas64bitDataLength() throws Exception
    {
        File orig = new File("testdata", "test218.mp4");
        if (!orig.isFile())
        {
            System.err.println("Unable to test file - not available");
            return;
        }

        Exception ex=null;
        try
        {
            File testFile = AbstractTestCase.copyAudioToTmp("test218.mp4");
            AudioFile af = AudioFileIO.read(testFile);
            assertNotNull(af.getTag());
            System.out.println(af.getTag());
            assertEquals("1968",(af.getTag().getFirst(FieldKey.YEAR)));
        }
        catch(Exception e)
        {
            e.printStackTrace();
            ex=e;
        }
        assertTrue(ex instanceof CannotReadException);
    }

    public void testReadCorruptMp4ThatLooksLikehas64bitDataLengthAtomTree() throws Exception
    {
        File orig = new File("testdata", "test218.mp4");
        if (!orig.isFile())
        {
            System.err.println("Unable to test file - not available");
            return;
        }

        try
        {
            Mp4AtomTree tree = new Mp4AtomTree(orig);
            tree.printAtomTree();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
