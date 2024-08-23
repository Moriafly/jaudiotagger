package org.jaudiotagger.issues;

import org.jaudiotagger.AbstractTestCase;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.mp4.Mp4AtomTree;

import java.io.File;

/**
 * Test writing mp4
 */
public class Issue387Test extends AbstractTestCase
{
    public void testIssue() throws Exception
    {
        Exception caught = null;
        try
        {
            File orig = new File("testdata", "test100.mp4");
            if (!orig.isFile())
            {
                System.err.println("Unable to test file - not available");
                return;
            }

            File testFile = AbstractTestCase.copyAudioToTmp("test100.mp4");
            AudioFile af = AudioFileIO.read(testFile);
            System.out.println(af.getAudioHeader());
            af.getTagOrCreateAndSetDefault();
            af.commit();

            Mp4AtomTree atomTree = new Mp4AtomTree(testFile);
            atomTree.printAtomTree();

        }
        catch(Exception e)
        {
            caught=e;
            e.printStackTrace();
        }
        assertNull(caught);
    }
}