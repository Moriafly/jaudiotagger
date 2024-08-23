/*
 *  MusicTag Copyright (C)2003,2004
 *
 *  This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser
 *  General Public  License as published by the Free Software Foundation; either version 2.1 of the License,
 *  or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with this library; if not,
 *  you can get a copy from http://www.opensource.org/licenses/lgpl-license.php or write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package com.xuncorp.guqin.media.tag.id3.framebody;

import com.xuncorp.guqin.media.tag.InvalidTagException;
import com.xuncorp.guqin.media.tag.id3.ID3v24Frames;
import com.xuncorp.guqin.media.tag.reference.Languages;

import java.nio.ByteBuffer;

/**
 * Language(s) Text information frame.
 * <p>The 'Language(s)' frame should contain the languages of the text or lyrics spoken or sung in the audio. The language is represented with three characters according to ISO-639-2. If more than one language is used in the text their language codes should follow according to their usage.
 *
 * <p>For more details, please refer to the ID3 specifications:
 * <ul>
 * <li><a href="http://www.id3.org/id3v2.3.0.txt">ID3 v2.3.0 Spec</a>
 * </ul>
 *
 * TODO:Although rare TLAN can actually return multiple language codes, at the moment they are all returned as a single
 * string via getText(), any additional parsing has to be done externally.
 *
 * @author : Paul Taylor
 * @author : Eric Farng
 * @version $Id$
 */
public class FrameBodyTLAN extends AbstractFrameBodyTextInfo implements ID3v24FrameBody, ID3v23FrameBody
{

    /**
     * Creates a new FrameBodyTLAN datatype.
     */
    public FrameBodyTLAN()
    {
        super();
    }

    public FrameBodyTLAN(FrameBodyTLAN body)
    {
        super(body);
    }

    /**
     * Creates a new FrameBodyTLAN datatype.
     *
     * @param textEncoding
     * @param text
     */
    public FrameBodyTLAN(byte textEncoding, String text)
    {
        super(textEncoding, text);
    }

    /**
     * Creates a new FrameBodyTLAN datatype.
     *
     * @param byteBuffer
     * @param frameSize
     * @throws InvalidTagException
     */
    public FrameBodyTLAN(ByteBuffer byteBuffer, int frameSize) throws InvalidTagException
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
        return ID3v24Frames.FRAME_ID_LANGUAGE;
    }

    /**
     *
     * @return true if text value is valid language code
     */
    public boolean isValid()
    {
        return Languages.getInstanceOf().getValueForId(getFirstTextValue())!=null;
    }
}
