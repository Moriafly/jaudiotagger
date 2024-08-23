package org.jaudiotagger.tag.id3;

import org.jaudiotagger.AbstractTestCase;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.id3.framebody.*;
import org.jaudiotagger.tag.id3.valuepair.TextEncoding;

import java.io.File;

/**
 * Test TMCL Frame
 */
public class FrameTMCLTest extends AbstractTestCase
{
    public static ID3v24Frame getInitialisedFrame()
    {
        ID3v24Frame frame = new ID3v24Frame(ID3v24Frames.FRAME_ID_MUSICIAN_CREDITS);
        FrameBodyTMCL fb = FrameBodyTMCLTest.getInitialisedBody();
        frame.setBody(fb);
        return frame;
    }

     public static ID3v24Frame getInitialisedFrameOdd()
    {
        ID3v24Frame frame = new ID3v24Frame(ID3v24Frames.FRAME_ID_MUSICIAN_CREDITS);
        FrameBodyTIPL fb = FrameBodyTIPLTest.getInitialisedBodyOdd();
        frame.setBody(fb);
        return frame;
    }

    public void testCreateID3v24Frame()
    {
        Exception exceptionCaught = null;
        ID3v24Frame frame = null;
        FrameBodyTMCL fb = null;
        try
        {
            frame = new ID3v24Frame(ID3v24Frames.FRAME_ID_MUSICIAN_CREDITS);
            fb = FrameBodyTMCLTest.getInitialisedBody();
            frame.setBody(fb);
        }
        catch (Exception e)
        {
            exceptionCaught = e;
        }

        assertNull(exceptionCaught);
        assertEquals(ID3v24Frames.FRAME_ID_MUSICIAN_CREDITS, frame.getIdentifier());
        assertEquals(TextEncoding.ISO_8859_1, fb.getTextEncoding());
        assertFalse(ID3v24Frames.getInstanceOf().isExtensionFrames(frame.getIdentifier()));
        assertTrue(ID3v24Frames.getInstanceOf().isSupportedFrames(frame.getIdentifier()));
        assertEquals(FrameBodyTMCLTest.MUSICIANS, fb.getText());

    }

    public void testCreateID3v23Frame()
    {
        Exception exceptionCaught = null;
        ID3v23Frame frame = null;
        FrameBodyTIPL fb = null;
        try
        {
            frame = new ID3v23Frame(ID3v23Frames.FRAME_ID_V3_INVOLVED_PEOPLE);
            fb = FrameBodyTIPLTest.getInitialisedBody();
            frame.setBody(fb);
        }
        catch (Exception e)
        {
            exceptionCaught = e;
        }

        assertNull(exceptionCaught);
        assertEquals(ID3v23Frames.FRAME_ID_V3_INVOLVED_PEOPLE, frame.getIdentifier());
        assertEquals(TextEncoding.ISO_8859_1, fb.getTextEncoding());
        assertFalse(ID3v23Frames.getInstanceOf().isExtensionFrames(frame.getIdentifier()));
        assertTrue(ID3v23Frames.getInstanceOf().isSupportedFrames(frame.getIdentifier()));
        assertEquals(FrameBodyTIPLTest.INVOLVED_PEOPLE, fb.getText());
    }

    public void testSaveToFile() throws Exception
    {
        File testFile = AbstractTestCase.copyAudioToTmp("testV1.mp3",new File("test1016.mp3"));
        MP3File mp3File = new MP3File(testFile);

        //Create and Save
        ID3v24Tag tag = new ID3v24Tag();
        tag.setFrame(FrameTMCLTest.getInitialisedFrame());
        mp3File.setID3v2Tag(tag);
        mp3File.save();

        //Reload
        mp3File = new MP3File(testFile);
        ID3v24Frame frame = (ID3v24Frame) mp3File.getID3v2Tag().getFrame(ID3v24Frames.FRAME_ID_MUSICIAN_CREDITS).get(0);
        FrameBodyTMCL body = (FrameBodyTMCL) frame.getBody();
        assertEquals(TextEncoding.ISO_8859_1, body.getTextEncoding());
    }

    public void testSaveToFileOdd() throws Exception
    {
        File testFile = AbstractTestCase.copyAudioToTmp("testV1.mp3",new File("test1016.mp3"));
        MP3File mp3File = new MP3File(testFile);

        //Create and Save
        ID3v24Tag tag = new ID3v24Tag();
        tag.setFrame(FrameTMCLTest.getInitialisedFrameOdd());
        mp3File.setID3v2Tag(tag);
        mp3File.save();

        //Reload
        mp3File = new MP3File(testFile);
        ID3v24Frame frame = (ID3v24Frame) mp3File.getID3v2Tag().getFrame(ID3v24Frames.FRAME_ID_MUSICIAN_CREDITS).get(0);
        FrameBodyTMCL body = (FrameBodyTMCL) frame.getBody();
        assertEquals(TextEncoding.ISO_8859_1, body.getTextEncoding());
    }

    public void testSaveEmptyFrameToFile() throws Exception
    {
        File testFile = AbstractTestCase.copyAudioToTmp("testV1.mp3",new File("test1004.mp3"));
        MP3File mp3File = new MP3File(testFile);

        ID3v24Frame frame = new ID3v24Frame(ID3v24Frames.FRAME_ID_INVOLVED_PEOPLE);
        frame.setBody(new FrameBodyTIPL());

        //Create and Save
        ID3v24Tag tag = new ID3v24Tag();
        tag.setFrame(frame);
        mp3File.setID3v2Tag(tag);
        mp3File.save();

        //Reload
        mp3File = new MP3File(testFile);
        frame = (ID3v24Frame) mp3File.getID3v2Tag().getFrame(ID3v24Frames.FRAME_ID_INVOLVED_PEOPLE).get(0);
        FrameBodyTIPL body = (FrameBodyTIPL) frame.getBody();
        assertEquals(TextEncoding.ISO_8859_1, body.getTextEncoding());
    }

    public void testConvertV24ToV23() throws Exception
    {
        File testFile = AbstractTestCase.copyAudioToTmp("testV1.mp3",new File("test1005.mp3"));
        MP3File mp3File = new MP3File(testFile);

        //Create and Save
        ID3v24Tag tag = new ID3v24Tag();
        tag.setFrame(FrameTMCLTest.getInitialisedFrame());

        mp3File.setID3v2Tag(tag);
        mp3File.save();

        //Reload and convertMetadata to v23 and save
        mp3File = new MP3File(testFile);
        ID3v23Tag v23Tag = new ID3v23Tag(mp3File.getID3v2TagAsv24());
        mp3File.setID3v2TagOnly(v23Tag);

        assertTrue(v23Tag.hasFrame("IPLS"));
        mp3File.save();

        //Reload
        mp3File = new MP3File(testFile);
        ID3v23Frame frame = (ID3v23Frame) mp3File.getID3v2Tag().getFrame(ID3v23Frames.FRAME_ID_V3_INVOLVED_PEOPLE).get(0);
        FrameBodyIPLS body = (FrameBodyIPLS) frame.getBody();
        assertEquals(TextEncoding.ISO_8859_1, body.getTextEncoding());
        assertEquals(FrameBodyTMCLTest.MUSICIANS, body.getText());
        assertEquals("violinist", body.getKeyAtIndex(0));
        assertEquals("eno,lanois", body.getValueAtIndex(0));
    }

    public void testConvertV24ToV23v2() throws Exception
    {
        File testFile = AbstractTestCase.copyAudioToTmp("testV1.mp3",new File("test1005.mp3"));
        MP3File mp3File = new MP3File(testFile);

        //Create and Save
        ID3v24Tag tag = new ID3v24Tag();
        tag.setFrame(FrameTMCLTest.getInitialisedFrame());
        tag.setFrame(FrameTMCLTest.getInitialisedFrame());

        mp3File.setID3v2Tag(tag);
        mp3File.save();

        //Reload and convertMetadata to v23 and save
        mp3File = new MP3File(testFile);
        ID3v23Tag v23Tag = new ID3v23Tag(mp3File.getID3v2TagAsv24());
        mp3File.setID3v2TagOnly(v23Tag);

        assertTrue(v23Tag.hasFrame("IPLS"));
        mp3File.save();

        //Reload
        mp3File = new MP3File(testFile);
        ID3v23Frame frame = (ID3v23Frame) mp3File.getID3v2Tag().getFrame(ID3v23Frames.FRAME_ID_V3_INVOLVED_PEOPLE).get(0);
        FrameBodyIPLS body = (FrameBodyIPLS) frame.getBody();
        assertEquals(TextEncoding.ISO_8859_1, body.getTextEncoding());
        assertEquals(FrameBodyTMCLTest.MUSICIANS, body.getText());
        assertEquals("violinist", body.getKeyAtIndex(0));
        assertEquals("eno,lanois", body.getValueAtIndex(0));
    }
}