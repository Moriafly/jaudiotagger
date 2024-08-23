package org.jaudiotagger.issues;

import org.jaudiotagger.AbstractTestCase;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.TagField;
import org.jaudiotagger.tag.id3.AbstractID3v2Frame;
import org.jaudiotagger.tag.id3.ID3v23Tag;
import org.jaudiotagger.tag.id3.ID3v24Frames;
import org.jaudiotagger.tag.id3.framebody.FrameBodyWOAR;

import java.io.File;
import java.util.List;

/**
 * Test
 */
public class Issue450Test extends AbstractTestCase
{
    public void testSaveUrl() throws Exception
    {
        File orig = new File("testdata", "test108.mp3");
        if (!orig.isFile())
        {
            System.err.println("Unable to test file - not available");
            return;
        }
        File testFile = AbstractTestCase.copyAudioToTmp("test108.mp3");
        MP3File mp3file = (MP3File) AudioFileIO.read(testFile);

        List<TagField> frames = mp3file.getID3v2TagAsv24().getFrame(ID3v24Frames.FRAME_ID_URL_ARTIST_WEB);
        AbstractID3v2Frame frame = (AbstractID3v2Frame)frames.get(0);
        assertNotNull(frame);

        frames = mp3file.getID3v2Tag().getFrame(ID3v24Frames.FRAME_ID_URL_ARTIST_WEB);
        frame = (AbstractID3v2Frame)frames.get(0);
        assertNotNull(frame);
        assertEquals(FrameBodyWOAR.class,frame.getBody().getClass());
        FrameBodyWOAR fb = (FrameBodyWOAR)frame.getBody();
        System.out.println(fb.getUrlLink());

        mp3file.setID3v2Tag(new ID3v23Tag(mp3file.getID3v2TagAsv24()));
        frames = mp3file.getID3v2Tag().getFrame(ID3v24Frames.FRAME_ID_URL_ARTIST_WEB);
        frame = (AbstractID3v2Frame)frames.get(0);
        assertNotNull(frame);
        assertEquals(FrameBodyWOAR.class,frame.getBody().getClass());
        fb = (FrameBodyWOAR)frame.getBody();
        System.out.println(fb.getUrlLink());
        mp3file.commit();




    }

}
