package org.jaudiotagger.tag.id3.framebody;

import org.jaudiotagger.AbstractTestCase;
import org.jaudiotagger.tag.id3.ID3v24Frames;
import org.jaudiotagger.tag.id3.valuepair.TextEncoding;

/**
 * Test TMCL
 */
public class FrameBodyTMCLTest extends AbstractTestCase
{
    public static final String MUSICIANS = "violinist\0eno,lanois";
    public static final String MUSICIANS_ODD = "violinist\0eno,lanois\0viola";

    public static FrameBodyTMCL getInitialisedBody()
    {
        FrameBodyTMCL fb = new FrameBodyTMCL();
        fb.setText(FrameBodyTMCLTest.MUSICIANS);
        return fb;
    }

    public void testCreateFrameBody()
    {
        Exception exceptionCaught = null;
        FrameBodyTMCL fb = null;
        try
        {
            fb = new FrameBodyTMCL();
            fb.setText(FrameBodyTMCLTest.MUSICIANS);
        }
        catch (Exception e)
        {
            exceptionCaught = e;
        }

        assertNull(exceptionCaught);
        assertEquals(ID3v24Frames.FRAME_ID_MUSICIAN_CREDITS, fb.getIdentifier());
        assertEquals(TextEncoding.ISO_8859_1, fb.getTextEncoding());
        assertEquals(FrameBodyTMCLTest.MUSICIANS, fb.getText());
        //assertEquals(2,fb.getNumberOfValues());
        //assertEquals("producer",fb.getNumberOfPairs());
        assertEquals("violinist",fb.getKeyAtIndex(0));
        assertEquals("eno,lanois",fb.getValueAtIndex(0));

    }

    public void testCreateFrameBodyodd()
    {
        Exception exceptionCaught = null;
        FrameBodyTMCL fb = null;
        try
        {
            fb = new FrameBodyTMCL();
            fb.setText(FrameBodyTMCLTest.MUSICIANS_ODD);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            exceptionCaught = e;
        }

        assertNull(exceptionCaught);
        assertEquals(ID3v24Frames.FRAME_ID_MUSICIAN_CREDITS, fb.getIdentifier());
        assertEquals(TextEncoding.ISO_8859_1, fb.getTextEncoding());
        assertEquals(FrameBodyTMCLTest.MUSICIANS, fb.getText());
        //assertEquals(2,fb.getNumberOfValues());
        //assertEquals("producer",fb.getNumberOfPairs());
        assertEquals("violinist",fb.getKeyAtIndex(0));
        assertEquals("eno,lanois",fb.getValueAtIndex(0));

    }
    public void testCreateFrameBodyEmptyConstructor()
    {
        Exception exceptionCaught = null;
        FrameBodyTMCL fb = null;
        try
        {
            fb = new FrameBodyTMCL();
            fb.setText(FrameBodyTMCLTest.MUSICIANS);
        }
        catch (Exception e)
        {
            exceptionCaught = e;
        }

        assertNull(exceptionCaught);
        assertEquals(ID3v24Frames.FRAME_ID_MUSICIAN_CREDITS, fb.getIdentifier());
        assertEquals(TextEncoding.ISO_8859_1, fb.getTextEncoding());
        assertEquals(FrameBodyTMCLTest.MUSICIANS, fb.getText());
    }
}