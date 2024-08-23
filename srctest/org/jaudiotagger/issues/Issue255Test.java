package org.jaudiotagger.issues;

import org.jaudiotagger.AbstractTestCase;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.mp4.Mp4AtomTree;
import org.jaudiotagger.tag.FieldKey;

import java.io.File;

/**
 * Test Writing to new urls with common interface
 */
public class Issue255Test extends AbstractTestCase
{
    /**
     * Test Mp4 with padding after last atom
     */
    public void testReadMp4FileWithPaddingAfterLastAtom()
    {
        File orig = new File("testdata", "test35.m4a");                 
        if (!orig.isFile())
        {
            System.err.println("Unable to test file - not available");
            return;
        }

        File testFile = null;
        Exception exceptionCaught = null;
        try
        {
            testFile = AbstractTestCase.copyAudioToTmp("test35.m4a");

            AudioFileIO.read(testFile);

            //Print Out Tree

        }
        catch(Exception e)
        {
            e.printStackTrace();
            exceptionCaught=e;
        }

        assertNull(exceptionCaught);

        try
        {
            //Now just createField tree
            Mp4AtomTree atomTree = new Mp4AtomTree(testFile);
            atomTree.printAtomTree();
        }
        catch(Exception e)
        {
            e.printStackTrace();
            exceptionCaught=e;
        }
        assertNull(exceptionCaught);
    }

    /**
     * Test to write all data to a m4p which has a padding but no MDAT Dat aso fails on read
     * <p/>    
     */
    public void testReadFileWithInvalidPadding()
    {
        File orig = new File("testdata", "test28.m4p");
        if (!orig.isFile())
        {
            System.err.println("Unable to test file - not available");
            return;
        }

        Exception exceptionCaught = null;
        try
        {
            File testFile = AbstractTestCase.copyAudioToTmp("test28.m4p", new File("WriteFileWithInvalidFreeAtom.m4p"));

            AudioFileIO.read(testFile);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            exceptionCaught = e;
        }
        assertTrue(exceptionCaught instanceof CannotReadException);
    }

    /**
     * Test Mp4 with padding after last atom
     */
    public void testWriteMp4FileWithPaddingAfterLastAtom()
    {
        File orig = new File("testdata", "test35.m4a");
        if (!orig.isFile())
        {
            System.err.println("Unable to test file - not available");
            return;
        }

        File testFile = null;
        Exception exceptionCaught = null;
        try
        {
            testFile = AbstractTestCase.copyAudioToTmp("test35.m4a");

            //Add a v24Tag
            AudioFile af = AudioFileIO.read(testFile);
            af.getTag().setField(FieldKey.ALBUM,"NewValue");
            af.commit();
        }
        catch(Exception e)
        {
            e.printStackTrace();
            exceptionCaught=e;
        }

        //Ensure temp file deleted
        File[] files = testFile.getParentFile().listFiles();
        for(File file:files)
        {
            System.out.println("Checking "+file.getName());
            assertFalse(file.getName().matches(".*test35.*.tmp"));
        }
        assertNull(exceptionCaught);
    }
}
