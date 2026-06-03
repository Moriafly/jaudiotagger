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

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.generic.AudioFileReader;
import org.jaudiotagger.audio.generic.GenericAudioHeader;
import org.jaudiotagger.audio.ogg.util.OpusInfoReader;
import org.jaudiotagger.tag.Tag;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.logging.Logger;

/**
 * Read Ogg Opus File Tag and Encoding information.
 *
 * Reads Opus audio files in an Ogg container. Opus uses VorbisComment tags
 * with the "OpusTags" magic signature instead of "vorbis".
 */
public class OggOpusFileReader extends AudioFileReader {
    // Logger Object
    public static Logger logger = Logger.getLogger("org.jaudiotagger.audio.ogg");

    private final OpusInfoReader ir;
    private final OggOpusTagReader vtr;

    public OggOpusFileReader() {
        ir = new OpusInfoReader();
        vtr = new OggOpusTagReader();
    }

    @Override
    protected GenericAudioHeader getEncodingInfo(RandomAccessFile raf)
            throws CannotReadException, IOException {
        return ir.read(raf);
    }

    @Override
    protected Tag getTag(RandomAccessFile raf) throws CannotReadException, IOException {
        return vtr.read(raf);
    }
}
