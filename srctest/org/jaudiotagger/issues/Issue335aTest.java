package org.jaudiotagger.issues;

import org.jaudiotagger.AbstractTestCase;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.id3.FrameIPLSTest;
import org.jaudiotagger.tag.id3.ID3v23Frame;
import org.jaudiotagger.tag.id3.ID3v23Tag;
import org.jaudiotagger.tag.id3.ID3v24Tag;
import org.jaudiotagger.tag.id3.framebody.FrameBodyIPLS;

import java.io.File;

/**
 * Test conversion from v23 to v24 of IPLS frame
 */
public class Issue335aTest extends AbstractTestCase
{
    /**
     * Data roels are Non-musicians so just create TIPL
     * @throws Exception
     */
    public void testConvertV23v24_v1() throws Exception
    {
        File testFile = AbstractTestCase.copyAudioToTmp("testV1.mp3",new File("test1005.mp3"));
        MP3File mp3File = new MP3File(testFile);

        //Create and Save an IPLS frame
        ID3v23Tag tag = new ID3v23Tag();
        ID3v23Frame frame = FrameIPLSTest.getInitialisedFrame();
        ((FrameBodyIPLS)frame.getBody()).setText("producer\0eno,lanois\0engineer\0lillywhite");
        tag.setFrame(frame);

        mp3File.setID3v2Tag(tag);
        mp3File.save();

        //Reload and convertMetadata to v24 and save, should convert to a TIPL frame
        mp3File = new MP3File(testFile);
        ID3v24Tag v24Tag = new ID3v24Tag(mp3File.getID3v2TagAsv24());
        mp3File.setID3v2TagOnly(v24Tag);

        assertTrue(v24Tag.hasFrame("TIPL"));
        assertFalse(v24Tag.hasFrame("TMCL"));
    }

    /**
     * Data roles is both so create both
     *
     * @throws Exception
     */
    public void testConvertV23v24_v2() throws Exception
    {
        File testFile = AbstractTestCase.copyAudioToTmp("testV1.mp3",new File("test1005.mp3"));
        MP3File mp3File = new MP3File(testFile);

        //Create and Save an IPLS frame
        ID3v23Tag tag = new ID3v23Tag();
        ID3v23Frame frame = FrameIPLSTest.getInitialisedFrame();
        ((FrameBodyIPLS)frame.getBody()).setText("violin\0eno,lanois\0engineer\0lillywhite");
        tag.setFrame(frame);

        mp3File.setID3v2Tag(tag);
        mp3File.save();

        //Reload and convertMetadata to v24 and save, should convert to a TIPL frame
        mp3File = new MP3File(testFile);
        ID3v24Tag v24Tag = new ID3v24Tag(mp3File.getID3v2TagAsv24());
        mp3File.setID3v2TagOnly(v24Tag);

        assertTrue(v24Tag.hasFrame("TIPL"));
        assertFalse(v24Tag.hasFrame("TMCL"));
    }

    /**
     * Data roles all musicians so just TMCL
     * @throws Exception
     */
    public void testConvertV23v24_v3() throws Exception
    {
        File testFile = AbstractTestCase.copyAudioToTmp("testV1.mp3",new File("test1005.mp3"));
        MP3File mp3File = new MP3File(testFile);

        //Create and Save an IPLS frame
        ID3v23Tag tag = new ID3v23Tag();
        ID3v23Frame frame = FrameIPLSTest.getInitialisedFrame();
        ((FrameBodyIPLS)frame.getBody()).setText("violin\0eno,lanois\0cello\0lillywhite");
        tag.setFrame(frame);

        mp3File.setID3v2Tag(tag);
        mp3File.save();

        //Reload and convertMetadata to v24 and save, should convert to a TIPL frame
        mp3File = new MP3File(testFile);
        ID3v24Tag v24Tag = new ID3v24Tag(mp3File.getID3v2TagAsv24());
        mp3File.setID3v2TagOnly(v24Tag);

        assertTrue(v24Tag.hasFrame("TIPL"));
        assertFalse(v24Tag.hasFrame("TMCL"));
    }


    /**
     * Data role unknown so just TIPL
     * @throws Exception
     */
    public void testConvertV23v24_v4() throws Exception
    {
        File testFile = AbstractTestCase.copyAudioToTmp("testV1.mp3",new File("test1005.mp3"));
        MP3File mp3File = new MP3File(testFile);

        //Create and Save an IPLS frame
        ID3v23Tag tag = new ID3v23Tag();
        ID3v23Frame frame = FrameIPLSTest.getInitialisedFrame();
        ((FrameBodyIPLS)frame.getBody()).setText("nonsense\0eno,lanois\0nonsense2\0lillywhite");
        tag.setFrame(frame);

        mp3File.setID3v2Tag(tag);
        mp3File.save();

        //Reload and convertMetadata to v24 and save, should convert to a TIPL frame
        mp3File = new MP3File(testFile);
        ID3v24Tag v24Tag = new ID3v24Tag(mp3File.getID3v2TagAsv24());
        mp3File.setID3v2TagOnly(v24Tag);

        assertTrue(v24Tag.hasFrame("TIPL"));
        assertFalse(v24Tag.hasFrame("TMCL"));
    }
}
