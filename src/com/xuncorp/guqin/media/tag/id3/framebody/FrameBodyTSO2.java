package com.xuncorp.guqin.media.tag.id3.framebody;

import com.xuncorp.guqin.media.tag.InvalidTagException;
import com.xuncorp.guqin.media.tag.id3.ID3v24Frames;

import java.nio.ByteBuffer;

/**
 * Album Artist Sort name  ( iTunes Only)
 */
public class FrameBodyTSO2 extends AbstractFrameBodyTextInfo implements ID3v24FrameBody, ID3v23FrameBody
{
    /**
     * Creates a new FrameBodyTSOA datatype.
     */
    public FrameBodyTSO2()
    {
    }

    public FrameBodyTSO2(FrameBodyTSO2 body)
    {
        super(body);
    }

    /**
     * Creates a new FrameBodyTSOA datatype.
     *
     * @param textEncoding
     * @param text
     */
    public FrameBodyTSO2(byte textEncoding, String text)
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
    public FrameBodyTSO2(ByteBuffer byteBuffer, int frameSize) throws InvalidTagException
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
        return ID3v24Frames.FRAME_ID_ALBUM_ARTIST_SORT_ORDER_ITUNES;
    }
}
