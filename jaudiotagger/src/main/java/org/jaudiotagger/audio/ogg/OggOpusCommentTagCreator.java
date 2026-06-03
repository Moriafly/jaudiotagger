/*
 * Entagged Audio Tag library
 * Copyright (c) 2003-2005 Raphaël Slinckx <raphael@slinckx.net>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.jaudiotagger.audio.ogg;

import org.jaudiotagger.audio.ogg.util.OpusIdentificationHeader;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentCreator;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

/**
 * Creates an OggOpus Comment Tag from a VorbisComment for use within an OggOpus Container.
 *
 * The OpusTags packet format is simply:
 *   ["OpusTags" (8 bytes)][vorbis_comment_data]
 *
 * There is NO separate packet type byte and NO framing bit for Opus (unlike Vorbis).
 */
public class OggOpusCommentTagCreator {
    // Logger Object
    public static Logger logger = Logger.getLogger("org.jaudiotagger.audio.ogg");

    private final VorbisCommentCreator creator = new VorbisCommentCreator();

    /**
     * Convert the tag to a raw OpusTags packet.
     *
     * Format: ['OpusTags'][vorbis_comment_data]
     *
     * @param tag the tag to convert
     * @return ByteBuffer containing the raw OpusTags packet
     * @throws UnsupportedEncodingException
     */
    public ByteBuffer convert(Tag tag) throws UnsupportedEncodingException {
        ByteBuffer ogg = creator.convertMetadata(tag);
        int tagLength = ogg.capacity() + OpusIdentificationHeader.OPUS_TAGS_MAGIC_LENGTH;

        ByteBuffer buf = ByteBuffer.allocate(tagLength);

        // ['OpusTags']
        buf.put(OpusIdentificationHeader.OPUS_TAGS_MAGIC_AS_BYTES);

        // The actual VorbisComment data (vendor string + user comments)
        buf.put(ogg);

        buf.rewind();
        return buf;
    }
}
