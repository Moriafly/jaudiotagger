/*
 * Entagged Audio Tag library
 * Copyright (c) 2004-2005 Christian Laireiter <liree@web.de>
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
package org.jaudiotagger.audio.asf;

import org.jaudiotagger.audio.asf.io.*;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.generic.AudioFileReader2;
import org.jaudiotagger.audio.generic.GenericAudioHeader;
import org.jaudiotagger.tag.Tag;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

/**
 * This reader can read ASF files containing any content (stream type). <br>
 *
 * @author Christian Laireiter
 */
public class AsfFileReader extends AudioFileReader2 {
    private final AsfInfoReader ir = new AsfInfoReader();
    private final AsfTagReader tr = new AsfTagReader();

    // -----------------------------------------------------------------------
    // AudioFileReader2 Implementation (Path based)
    // -----------------------------------------------------------------------

    @Override
    protected GenericAudioHeader getEncodingInfo(Path path) throws CannotReadException, IOException {
        try (FileChannel fc = FileChannel.open(path)) {
            return ir.read(fc, path.toString());
        }
    }

    @Override
    protected Tag getTag(Path path) throws CannotReadException, IOException {
        try (FileChannel fc = FileChannel.open(path)) {
            return tr.read(fc, path.toString());
        }
    }
}
