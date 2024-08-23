package org.jaudiotagger.issues;

import org.jaudiotagger.AbstractTestCase;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.mp4.Mp4AtomTree;
import org.jaudiotagger.tag.FieldKey;

import java.io.File;

/**
 * Test
 */
public class Issue463Test extends AbstractTestCase
{
    public void testReadMp4() throws Exception
    {
        Exception ex=null;
        try
        {
            File orig = new File("testdata", "test116.m4a");
            if (!orig.isFile())
            {
                System.err.println("Unable to test file - not available");
                return;
            }


            File testFile = AbstractTestCase.copyAudioToTmp("test116.m4a");
            Mp4AtomTree tree = new Mp4AtomTree(testFile, false);
            tree.printAtomTree();

            AudioFile af = AudioFileIO.read(testFile);
            assertNotNull(af.getTag());
            assertEquals("Zbigniew Preisner", af.getTag().getFirst(FieldKey.ARTIST));

            af.getTag().setField(FieldKey.ARTIST,"fred");
            assertEquals("fred",af.getTag().getFirst(FieldKey.ARTIST));
            af.commit();


            tree = new Mp4AtomTree(testFile,false);
            tree.printAtomTree();

            af = AudioFileIO.read(testFile);
            assertNotNull(af.getTag());
            assertEquals("fred",af.getTag().getFirst(FieldKey.ARTIST));

        }
        catch(Exception e)
        {
            e.printStackTrace();
            ex=e;
        }
        assertNull(ex);
    }
}
