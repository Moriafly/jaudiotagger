package org.jaudiotagger.issues;

import org.jaudiotagger.AbstractTestCase;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.mp3.MP3AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagOptionSingleton;
import java.io.File;

/**
 * Testing shrinking of padding data
 */
public class Issue301Test extends AbstractTestCase
{
    public void testWriteLessDataAndShrink() throws Exception
    {
        File orig = new File("testdata", "test47.mp3");
        if (!orig.isFile())
        {
            System.err.println("Unable to test file - not available");
            return;
        }

        Exception ex=null;
        try
        {
            final int AUDIO_LENGTH = 113265;

            File testFile = AbstractTestCase.copyAudioToTmp("test47.mp3",new File("testStripPadding.mp3"));
            AudioFile af = AudioFileIO.read(testFile);
            assertNotNull(af.getTag());
            System.out.println(af.getTag());
            assertEquals(161,((MP3AudioHeader)af.getAudioHeader()).getMp3StartByte());
            assertEquals(161 + AUDIO_LENGTH,testFile.length());

            //Shorten data but dont request to shrink so same size
            TagOptionSingleton.getInstance().setId3v2PaddingWillShorten(false);
            af.getTag().setField(FieldKey.ALBUM,"Shorter");
            af.commit();
            af = AudioFileIO.read(testFile);
            assertNotNull(af.getTag());
            System.out.println(af.getTag());
            assertEquals(161,((MP3AudioHeader)af.getAudioHeader()).getMp3StartByte());
            assertEquals(161 + AUDIO_LENGTH,testFile.length());

            //Now shorter and request to shrunk so removes all padding
            TagOptionSingleton.getInstance().setId3v2PaddingWillShorten(true);
            af.getTag().setField(FieldKey.ALBUM,"Shorter");
            af.commit();
            af = AudioFileIO.read(testFile);
            assertNotNull(af.getTag());
            System.out.println(af.getTag());
            assertEquals(127,((MP3AudioHeader)af.getAudioHeader()).getMp3StartByte());
            assertEquals(127 + AUDIO_LENGTH,testFile.length());

            //Now a bit longer because more data needed but request to shorten so no spare padding added
            af.getTag().setField(FieldKey.ALBUM,"SlightlyLonger");
            af.commit();
            af = AudioFileIO.read(testFile);
            assertNotNull(af.getTag());
            System.out.println(af.getTag());
            assertEquals(134,((MP3AudioHeader)af.getAudioHeader()).getMp3StartByte());
            assertEquals(134 + AUDIO_LENGTH,testFile.length());

            //Now a bit longer and because have to rewrite audio data anyway we add some spare padding
            TagOptionSingleton.getInstance().setId3v2PaddingWillShorten(false);
            af.getTag().setField(FieldKey.ALBUM,"SoSlightlyLonger");
            af.commit();
            af = AudioFileIO.read(testFile);
            assertNotNull(af.getTag());
            System.out.println(af.getTag());
            assertEquals(236,((MP3AudioHeader)af.getAudioHeader()).getMp3StartByte());
            assertEquals(236 + AUDIO_LENGTH,testFile.length());

            //Nowshorter and set remove padding
            TagOptionSingleton.getInstance().setId3v2PaddingWillShorten(true);
            af.getTag().setField(FieldKey.ALBUM,"SlightlyLonger");
            af.commit();
            af = AudioFileIO.read(testFile);
            assertNotNull(af.getTag());
            System.out.println(af.getTag());
            assertEquals(134,((MP3AudioHeader)af.getAudioHeader()).getMp3StartByte());
            assertEquals(134 + AUDIO_LENGTH,testFile.length());
        }
        catch(Exception e)
        {
            e.printStackTrace();
            ex=e;
        }
        assertNull(ex);
    }

    public void testWriteLessDataAndShrink2() throws Exception
    {
        File orig = new File("testdata", "test308.mp3");
        if (!orig.isFile())
        {
            System.err.println("Unable to test file - not available");
            return;
        }

        Exception ex=null;
        try
        {
            final int AUDIO_LENGTH = 5048112;
            File testFile = AbstractTestCase.copyAudioToTmp("test308.mp3",new File("test308.mp3"));
            AudioFile af = AudioFileIO.read(testFile);
            assertNotNull(af.getTag());
            assertEquals(1856886,((MP3AudioHeader)af.getAudioHeader()).getMp3StartByte());
            assertEquals(1856886 + AUDIO_LENGTH, testFile.length());
            System.out.println(af.getTag());

            TagOptionSingleton.getInstance().setId3v2PaddingWillShorten(true);
            af.getTag().deleteArtworkField();
            af.commit();
            af = AudioFileIO.read(testFile);
            assertNotNull(af.getTag());
            System.out.println(af.getTag());
            assertEquals(446,((MP3AudioHeader)af.getAudioHeader()).getMp3StartByte());
            assertEquals(446 + AUDIO_LENGTH, testFile.length());
        }
        catch(Exception e)
        {
            e.printStackTrace();
            ex=e;
        }
        assertNull(ex);
    }
}
