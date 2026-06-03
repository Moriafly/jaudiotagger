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
import org.jaudiotagger.audio.ogg.util.OggPageHeader;
import org.jaudiotagger.audio.ogg.util.OpusIdentificationHeader;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentReader;
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Logger;

/**
 * Read Opus Tag (Vorbis Comment) within an Ogg container.
 *
 * Opus uses the "OpusTags" magic signature for its comment header directly
 * as the page data (no separate packet type byte), followed by standard
 * VorbisComment data. Unlike Vorbis, Opus has no Setup header — only the
 * OpusHead (identification) and OpusTags (comment) headers.
 *
 * The OpusTags (comment header) page data format is:
 *   ["OpusTags" (8 bytes)][vendor_length (4 bytes LE)][vendor_string (UTF-8)]
 *   [user_comment_list_length (4 bytes LE)][user_comment_1]...[user_comment_N]
 */
public class OggOpusTagReader {
    // Logger Object
    public static Logger logger = Logger.getLogger("org.jaudiotagger.audio.ogg");

    private final VorbisCommentReader vorbisCommentReader;

    public OggOpusTagReader() {
        vorbisCommentReader = new VorbisCommentReader();
    }

    /**
     * Read the OpusTags (Vorbis Comment) from the file.
     *
     * @param raf the random access file to read from
     * @return the VorbisCommentTag or an empty tag if no OpusTags header is found
     * @throws CannotReadException if there is an error reading the data
     * @throws IOException         if an I/O error occurs
     */
    public Tag read(RandomAccessFile raf) throws CannotReadException, IOException {
        logger.config("Starting to read ogg opus tag from file:");
        byte[] rawOpusCommentData = readRawPacketData(raf);

        // Begin tag reading - no framing bit for Opus
        VorbisCommentTag tag = vorbisCommentReader.read(rawOpusCommentData, false);
        logger.fine("Completed Read Opus Comment Tag");
        return tag;
    }

    /**
     * Retrieve the raw VorbisComment data from the OpusTags packet.
     *
     * @param raf the random access file
     * @return raw VorbisComment data bytes (after the "OpusTags" magic)
     * @throws CannotReadException if unable to find OpusHead or OpusTags headers
     * @throws IOException         if an I/O error occurs
     */
    public byte[] readRawPacketData(RandomAccessFile raf) throws CannotReadException, IOException {
        logger.fine("Read 1st Ogg page (should contain OpusHead)");

        // 1st page = OpusHead (identification header)
        OggPageHeader pageHeader = OggPageHeader.read(raf);

        // Verify it contains an OpusHead (first 8 bytes of page data = "OpusHead")
        long headerDataStart = raf.getFilePointer();
        byte[] checkMagic = new byte[OpusIdentificationHeader.OPUS_HEAD_MAGIC_LENGTH];
        raf.read(checkMagic);
        String magic = new String(checkMagic, StandardCharsets.ISO_8859_1);
        if (!OpusIdentificationHeader.OPUS_HEAD_MAGIC.equals(magic)) {
            throw new CannotReadException("Cannot find OpusHead header at first Ogg page, got: " + magic);
        }

        // Skip over data to end of first page
        raf.seek(headerDataStart + pageHeader.getPageLength());

        logger.fine("Read 2nd page (should contain OpusTags)");

        // 2nd page = OpusTags, may extend to additional pages
        pageHeader = OggPageHeader.read(raf);

        // Now at start of page data — check for "OpusTags" magic (8 bytes at offset 0)
        byte[] b = new byte[OpusIdentificationHeader.OPUS_TAGS_MAGIC_LENGTH];
        raf.read(b);
        if (!isOpusCommentHeader(b)) {
            // No OpusTags metadata found — this can happen with freshly encoded files
            logger.warning("No OpusTags metadata found at expected position, returning empty tag");
            return new byte[0];
        }

        // Convert the comment raw data which may span multiple pages back into a raw packet
        byte[] rawOpusCommentData = convertToOpusCommentPacket(pageHeader, raf);
        return rawOpusCommentData;
    }

    /**
     * Check if the page data starts with an OpusTags (comment) header.
     *
     * @param headerData the first bytes of page data
     * @return true if this is an OpusTags comment header
     */
    public boolean isOpusCommentHeader(byte[] headerData) {
        if (headerData.length < OpusIdentificationHeader.OPUS_TAGS_MAGIC_LENGTH) {
            return false;
        }
        String magic = new String(headerData, 0, OpusIdentificationHeader.OPUS_TAGS_MAGIC_LENGTH, StandardCharsets.ISO_8859_1);
        return OpusIdentificationHeader.OPUS_TAGS_MAGIC.equals(magic);
    }

    /**
     * Extract the raw VorbisComment data from the OpusTags packet spanning multiple Ogg pages.
     *
     * The RandomAccessFile should be positioned right after the "OpusTags" magic bytes.
     *
     * @param startOpusCommentPage the Ogg page containing the start of the OpusTags packet
     * @param raf                  the random access file, positioned after the "OpusTags" magic
     * @return raw VorbisComment data bytes
     * @throws IOException         if an I/O error occurs
     * @throws CannotReadException if the data cannot be read
     */
    private byte[] convertToOpusCommentPacket(OggPageHeader startOpusCommentPage, RandomAccessFile raf)
            throws IOException, CannotReadException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        List<OggPageHeader.PacketStartAndLength> packetList = startOpusCommentPage.getPacketList();
        int firstPacketLength = packetList.get(0).getLength();

        // The raw comment data is the page data minus the "OpusTags" magic (8 bytes)
        int headerOverhead = OpusIdentificationHeader.OPUS_TAGS_MAGIC_LENGTH;
        int commentDataLength = firstPacketLength - headerOverhead;

        byte[] b = new byte[commentDataLength];
        raf.read(b);
        baos.write(b);

        // If there's another packet on this page, the comment packet is complete
        if (packetList.size() > 1) {
            logger.config("OpusTags finish on this page because there is another packet");
            return baos.toByteArray();
        }

        // If the last packet is complete, we're done
        if (!startOpusCommentPage.isLastPacketIncomplete()) {
            logger.config("OpusTags finish on this page because the packet is complete");
            return baos.toByteArray();
        }

        // The OpusTags packet extends to the next page(s)
        while (true) {
            logger.config("Reading next page for continued OpusTags");
            OggPageHeader nextPageHeader = OggPageHeader.read(raf);
            packetList = nextPageHeader.getPacketList();
            b = new byte[packetList.get(0).getLength()];
            raf.read(b);
            baos.write(b);

            if (packetList.size() > 1) {
                logger.config("OpusTags finish on page because there is another packet");
                return baos.toByteArray();
            }

            if (!nextPageHeader.isLastPacketIncomplete()) {
                logger.config("OpusTags finish on page because this packet is complete");
                return baos.toByteArray();
            }
        }
    }
}
