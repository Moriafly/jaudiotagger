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

package org.jaudiotagger.audio.flac;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.flac.metadatablock.MetadataBlockDataPicture;
import org.jaudiotagger.audio.flac.metadatablock.MetadataBlockHeader;
import org.jaudiotagger.logging.Hex;
import org.jaudiotagger.tag.InvalidFrameException;
import org.jaudiotagger.tag.flac.FlacTag;
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentReader;
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Read Flac Tag.
 */
public class FlacTagReader {
    public static Logger logger = Logger.getLogger("org.jaudiotagger.audio.flac");

    private final VorbisCommentReader vorbisCommentReader = new VorbisCommentReader();

    public FlacTag read(Path path) throws CannotReadException, IOException {
        try (FileChannel fc = FileChannel.open(path)) {
            return read(fc);
        }
    }

    public FlacTag read(FileChannel fc) throws CannotReadException, IOException {
        FlacStreamReader flacStream = new FlacStreamReader(fc);
        flacStream.findStream();

        // Hold the metadata
        VorbisCommentTag tag = null;
        List<MetadataBlockDataPicture> images = new ArrayList<>();

        // Seems like we have a valid stream
        boolean isLastBlock = false;
        while (!isLastBlock) {
            if (logger.isLoggable(Level.CONFIG)) {
                logger.config("Looking for MetaBlockHeader at: " + fc.position());
            }

            // Read the header
            MetadataBlockHeader mbh = MetadataBlockHeader.readHeader(fc);

            if (logger.isLoggable(Level.CONFIG)) {
                logger.config("Reading MetadataBlockHeader: " + mbh + " ending at " + fc.position());
            }

            // Is it one containing some sort of metadata, therefore interested in it?

            // JAUDIOTAGGER-466:CBlocktype can be null
            if (mbh.getBlockType() != null) {
                switch (mbh.getBlockType()) {
                    // We got a vorbiscomment comment block, parse it
                    case VORBIS_COMMENT:
                        ByteBuffer commentHeaderRawPacket = ByteBuffer.allocate(mbh.getDataLength());
                        fc.read(commentHeaderRawPacket);
                        tag = vorbisCommentReader.read(commentHeaderRawPacket.array(), false);
                        break;

                    case PICTURE:
                        try {
                            MetadataBlockDataPicture mbdp = new MetadataBlockDataPicture(mbh, fc);
                            images.add(mbdp);
                        } catch (IOException | InvalidFrameException e) {
                            logger.warning("Unable to read picture metablock, ignoring: " + e.getMessage());
                        }
                        break;

                    case SEEKTABLE:
                        try {
                            long pos = fc.position();
                            fc.position(pos + mbh.getDataLength());
                        } catch (IOException e) {
                            logger.warning("Unable to read seek metablock, ignoring: " + e.getMessage());
                        }
                        break;

                    // This is not a metadata block we are interested in so we skip to next block
                    default:
                        if (logger.isLoggable(Level.CONFIG)) {
                            logger.config("Ignoring MetadataBlock: " + mbh.getBlockType());
                        }
                        fc.position(fc.position() + mbh.getDataLength());
                        break;
                }
            }
            isLastBlock = mbh.isLastBlock();
        }
        logger.config("Audio should start at:" + Hex.asHex(fc.position()));

        // Note there may not be either a tag or any images, no problem this is valid however to
        // make it easier we
        // just initialize Flac with an empty VorbisTag
        if (tag == null) {
            tag = VorbisCommentTag.createNewTag();
        }

        return new FlacTag(tag, images);
    }
}
