package com.xuncorp.guqin.media.tag.id3.framebody;

import com.xuncorp.guqin.media.tag.InvalidTagException;
import com.xuncorp.guqin.media.tag.id3.ID3v24Frames;

import java.nio.ByteBuffer;

/**
 * Composer Sort name (iTunes Only)
 */
public class FrameBodyTSOC extends AbstractFrameBodyTextInfo implements ID3v24FrameBody, ID3v23FrameBody
{
    /**
     * Creates a new FrameBodyTSOC datatype.
     */
    public FrameBodyTSOC()
    {
    }

    public FrameBodyTSOC(FrameBodyTSOC body)
    {
        super(body);
    }

    /**
     * Creates a new FrameBodyTSOA datatype.
     *
     * @param textEncoding
     * @param text
     */
    public FrameBodyTSOC(byte textEncoding, String text)
    {
        super(textEncoding, text);
    }

    /**
     * Creates a new FrameBodyTSOA datatype.
     *
     * @param byteBuffer
     * @param frameSize
     * @throws InvalidTagException
     */
    public FrameBodyTSOC(ByteBuffer byteBuffer, int frameSize) throws InvalidTagException
    {
        super(byteBuffer, frameSize);
    }

    /**
     * The ID3v2 frame identifier
     *
     * @return the ID3v2 frame identifier  for this frame type
     */
    public String getIdentifier()
    {
        return ID3v24Frames.FRAME_ID_COMPOSER_SORT_ORDER_ITUNES;
    }
}
