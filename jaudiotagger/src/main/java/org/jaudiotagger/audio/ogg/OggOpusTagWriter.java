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
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.generic.Utils;
import org.jaudiotagger.audio.ogg.util.OggCRCFactory;
import org.jaudiotagger.audio.ogg.util.OggPageHeader;
import org.jaudiotagger.audio.ogg.util.OpusIdentificationHeader;
import org.jaudiotagger.audio.ogg.util.VorbisHeader;


import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.id3.AbstractID3v1Tag;
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import java.util.List;
import java.util.logging.Logger;

/**
 * Write VorbisComment tag within an Ogg Opus file.
 *
 * Opus in Ogg has only two header packets: OpusHead (page 0) and OpusTags (page 1+).
 * There is no setup header, which makes the writing logic simpler than for Vorbis.
 *
 * The strategy is:
 *   1. Write the first Ogg page (OpusHead) unchanged
 *   2. Write the new OpusTags packet on the second page
 *   3. Copy remaining audio pages, renumbering their page sequences
 */
public class OggOpusTagWriter {
    // Logger Object
    public static Logger logger = Logger.getLogger("org.jaudiotagger.audio.ogg");

    private final OggOpusCommentTagCreator tc = new OggOpusCommentTagCreator();
    private final OggOpusTagReader reader = new OggOpusTagReader();

    /**
     * Delete the tag by writing an empty VorbisCommentTag.
     *
     * @param raf     the original file
     * @param tempRaf the temporary file to write to
     * @throws IOException         if an I/O error occurs
     * @throws CannotReadException if the file cannot be read
     * @throws CannotWriteException if the file cannot be written
     */
    public void delete(RandomAccessFile raf, RandomAccessFile tempRaf)
            throws IOException, CannotReadException, CannotWriteException {
        try {
            reader.read(raf);
        } catch (CannotReadException e) {
            // No existing tag, write empty one
            write(VorbisCommentTag.createNewTag(), raf, tempRaf);
            return;
        }

        VorbisCommentTag emptyTag = VorbisCommentTag.createNewTag();
        raf.seek(0);
        write(emptyTag, raf, tempRaf);
    }

    /**
     * Write the tag to the file.
     *
     * @param tag     the tag to write
     * @param raf     the original file
     * @param rafTemp the temporary file to write to
     * @throws CannotReadException  if the file cannot be read
     * @throws CannotWriteException if the file cannot be written
     * @throws IOException          if an I/O error occurs
     */
    public void write(Tag tag, RandomAccessFile raf, RandomAccessFile rafTemp)
            throws CannotReadException, CannotWriteException, IOException {
        logger.config("Starting to write Opus file:");

        // 1st Page: OpusHead (Identification Header) — write unchanged
        logger.fine("Read 1st Page: OpusHead");
        OggPageHeader pageHeader = OggPageHeader.read(raf);
        raf.seek(pageHeader.getStartByte());

        // Write 1st page unchanged
        long firstPageHeaderLength = OggPageHeader.OGG_PAGE_HEADER_FIXED_LENGTH
                + pageHeader.getSegmentTable().length;
        long firstPageTotalLength = firstPageHeaderLength + pageHeader.getPageLength();
        rafTemp.getChannel().transferFrom(raf.getChannel(), 0, firstPageTotalLength);
        rafTemp.skipBytes((int) firstPageTotalLength);
        logger.fine("Written OpusHead page");

        // 2nd page: OpusTags (may also contain audio data)
        OggPageHeader secondPageHeader = OggPageHeader.read(raf);
        long secondPageDataStart = raf.getFilePointer();
        logger.fine("Read 2nd Page: OpusTags and possibly audio");

        int oldSecondPageSequence = secondPageHeader.getPageSequence();

        // Get the raw OpusTags data from original file
        long savedFilePointer = raf.getFilePointer();
        raf.seek(0);
        OpusHeaderSizes opusHeaderSizes = readOpusHeaderSizes(raf);
        raf.seek(savedFilePointer);

        // Convert the new tag to raw packet data
        ByteBuffer newComment = tc.convert(tag);
        int newCommentLength = newComment.capacity();

        // Old comment size (including packet type and magic overhead)
        int oldCommentSize = opusHeaderSizes.commentHeaderSize;
        int commentOverhead = OpusIdentificationHeader.OPUS_TAGS_MAGIC_LENGTH;
        int oldRawCommentSize = oldCommentSize - commentOverhead;

        logger.fine("Old OpusTags raw size: " + oldCommentSize
                + ", New OpusTags size: " + newCommentLength);

        // Check if the comment header was found
        boolean commentExists = oldCommentSize > 0;

        // Calculate new second page data length
        int newSecondPageDataLength;
        if (commentExists) {
            // Replace: new comment + any extra data that followed the old comment
            newSecondPageDataLength = newCommentLength + opusHeaderSizes.extraPacketDataSize;
        } else {
            // Insert: new comment + existing page data
            newSecondPageDataLength = newCommentLength + secondPageHeader.getPageLength();
        }

        // Check if new comment fits on a single page alongside any extra packets
        if (isFitsOnASinglePage(newCommentLength, opusHeaderSizes.extraPacketList)) {
            logger.fine("OpusTags fits on single page");
            writeSingleSecondPage(newComment, newCommentLength, newSecondPageDataLength,
                    secondPageHeader, opusHeaderSizes, oldCommentSize, commentExists, raf, rafTemp);
        } else {
            logger.fine("OpusTags needs multiple pages");
            writeMultiplePages(newComment, newCommentLength, secondPageHeader,
                    opusHeaderSizes, oldCommentSize, commentExists, raf, rafTemp);
        }
    }

    /**
     * Write when the new comment fits on a single page.
     */
    private void writeSingleSecondPage(ByteBuffer newComment, int newCommentLength,
                                        int newSecondPageDataLength, OggPageHeader secondPageHeader,
                                        OpusHeaderSizes sizes, int oldCommentSize,
                                        boolean commentExists,
                                        RandomAccessFile raf, RandomAccessFile rafTemp)
            throws IOException, CannotReadException, CannotWriteException {

        int pageSequence = secondPageHeader.getPageSequence();

        // Build the new second page
        ByteBuffer secondPageBuffer = buildSecondPage(
                newComment, newCommentLength, newSecondPageDataLength,
                secondPageHeader, sizes.extraPacketList);

        // Write the new comment data
        secondPageBuffer.put(newComment);

        // Add extra packets (audio data that followed the old comment on the same page)
        if (commentExists && sizes.extraPacketDataSize > 0) {
            // Read past the old comment first
            raf.seek(sizes.commentHeaderStartPosition);
            OggPageHeader oldSecondPage = OggPageHeader.read(raf);
            raf.skipBytes(sizes.commentHeaderSize);
            byte[] extraData = new byte[sizes.extraPacketDataSize];
            raf.read(extraData);
            secondPageBuffer.put(extraData);
        } else if (!commentExists) {
            // Copy all data from the original second page after the OpusTags packet
            raf.seek(sizes.commentHeaderStartPosition);
            OggPageHeader oldSecondPage = OggPageHeader.read(raf);
            // Skip to after page header to copy all page data
            byte[] remainingData = new byte[oldSecondPage.getPageLength()];
            raf.read(remainingData);
            secondPageBuffer.put(remainingData);
        }

        calculateChecksumOverPage(secondPageBuffer);
        rafTemp.getChannel().write(secondPageBuffer);

        // Write remaining pages with renumbered sequences
        writeRemainingPages(pageSequence, raf, rafTemp);
    }

    /**
     * Write when the new comment needs multiple pages.
     */
    private void writeMultiplePages(ByteBuffer newComment, int newCommentLength,
                                     OggPageHeader secondPageHeader, OpusHeaderSizes sizes,
                                     int oldCommentSize, boolean commentExists,
                                     RandomAccessFile raf, RandomAccessFile rafTemp)
            throws IOException, CannotReadException, CannotWriteException {

        int pageSequence = secondPageHeader.getPageSequence();

        // Write full pages of comment data
        int offset = 0;
        int fullPages = newCommentLength / OggPageHeader.MAXIMUM_PAGE_DATA_SIZE;
        for (int i = 0; i < fullPages; i++) {
            byte[] segmentTable = createSegments(OggPageHeader.MAXIMUM_PAGE_DATA_SIZE, false);
            int pageHeaderLength = OggPageHeader.OGG_PAGE_HEADER_FIXED_LENGTH + segmentTable.length;
            ByteBuffer pageBuffer = ByteBuffer.allocate(pageHeaderLength + OggPageHeader.MAXIMUM_PAGE_DATA_SIZE);
            pageBuffer.order(ByteOrder.LITTLE_ENDIAN);

            pageBuffer.put(secondPageHeader.getRawHeaderData(), 0, OggPageHeader.OGG_PAGE_HEADER_FIXED_LENGTH - 1);
            pageBuffer.put((byte) segmentTable.length);
            for (byte st : segmentTable) {
                pageBuffer.put(st);
            }

            ByteBuffer commentSlice = newComment.slice();
            commentSlice.position(offset);
            commentSlice.limit(OggPageHeader.MAXIMUM_PAGE_DATA_SIZE);
            pageBuffer.put(commentSlice);

            pageBuffer.putInt(OggPageHeader.FIELD_PAGE_SEQUENCE_NO_POS, pageSequence);
            pageSequence++;

            if (i > 0) {
                pageBuffer.put(OggPageHeader.FIELD_HEADER_TYPE_FLAG_POS,
                        OggPageHeader.HeaderTypeFlag.CONTINUED_PACKET.getFileValue());
            }

            calculateChecksumOverPage(pageBuffer);
            rafTemp.getChannel().write(pageBuffer);
            offset += OggPageHeader.MAXIMUM_PAGE_DATA_SIZE;
        }

        // Write the last part of the comment
        int remaining = newCommentLength % OggPageHeader.MAXIMUM_PAGE_DATA_SIZE;
        if (remaining == 0) {
            remaining = OggPageHeader.MAXIMUM_PAGE_DATA_SIZE;
        }

        byte[] segmentTable = createSegments(remaining, true);
        int pageHeaderLength = OggPageHeader.OGG_PAGE_HEADER_FIXED_LENGTH + segmentTable.length;
        int extraDataSize = commentExists ? sizes.extraPacketDataSize
                : (sizes.extraPacketDataSize > 0 ? sizes.extraPacketDataSize : 0);

        ByteBuffer pageBuffer = ByteBuffer.allocate(pageHeaderLength + remaining + extraDataSize);
        pageBuffer.order(ByteOrder.LITTLE_ENDIAN);

        pageBuffer.put(secondPageHeader.getRawHeaderData(), 0, OggPageHeader.OGG_PAGE_HEADER_FIXED_LENGTH - 1);
        pageBuffer.put((byte) segmentTable.length);
        for (byte st : segmentTable) {
            pageBuffer.put(st);
        }

        newComment.position(offset);
        ByteBuffer remainingComment = newComment.slice();
        remainingComment.limit(remaining);
        pageBuffer.put(remainingComment);

        // Add extra data
        if (commentExists && sizes.extraPacketDataSize > 0) {
            raf.seek(sizes.commentHeaderStartPosition);
            OggPageHeader.read(raf);
            raf.skipBytes(sizes.commentHeaderSize);
            byte[] extraData = new byte[sizes.extraPacketDataSize];
            raf.read(extraData);
            pageBuffer.put(extraData);
        }

        pageBuffer.putInt(OggPageHeader.FIELD_PAGE_SEQUENCE_NO_POS, pageSequence);
        if (fullPages > 0) {
            pageBuffer.put(OggPageHeader.FIELD_HEADER_TYPE_FLAG_POS,
                    OggPageHeader.HeaderTypeFlag.CONTINUED_PACKET.getFileValue());
        }

        calculateChecksumOverPage(pageBuffer);
        rafTemp.getChannel().write(pageBuffer);

        writeRemainingPages(pageSequence, raf, rafTemp);
    }

    /**
     * Build a basic second page buffer with header set up.
     */
    private ByteBuffer buildSecondPage(ByteBuffer newComment, int newCommentLength,
                                        int newSecondPageDataLength, OggPageHeader secondPageHeader,
                                        List<OggPageHeader.PacketStartAndLength> extraPackets) {
        byte[] segmentTable = createSegmentTable(newCommentLength, extraPackets);
        int pageHeaderLength = OggPageHeader.OGG_PAGE_HEADER_FIXED_LENGTH + segmentTable.length;
        logger.fine("New second page header length: " + pageHeaderLength);

        ByteBuffer pageBuffer = ByteBuffer.allocate(pageHeaderLength + newSecondPageDataLength);
        pageBuffer.order(ByteOrder.LITTLE_ENDIAN);

        // Copy original page header data up to segment count
        pageBuffer.put(secondPageHeader.getRawHeaderData(), 0,
                OggPageHeader.OGG_PAGE_HEADER_FIXED_LENGTH - 1);
        // Number of page segments
        pageBuffer.put((byte) segmentTable.length);
        // Segment table
        for (byte st : segmentTable) {
            pageBuffer.put(st);
        }

        return pageBuffer;
    }

    /**
     * Calculate CRC checksum over a page buffer and update it.
     */
    private void calculateChecksumOverPage(ByteBuffer page) {
        page.putInt(OggPageHeader.FIELD_PAGE_CHECKSUM_POS, 0);
        byte[] crc = OggCRCFactory.computeCRC(page.array());
        for (int i = 0; i < crc.length; i++) {
            page.put(OggPageHeader.FIELD_PAGE_CHECKSUM_POS + i, crc[i]);
        }
        page.rewind();
    }

    /**
     * Write all remaining pages with renumbered page sequence numbers.
     */
    public void writeRemainingPages(int pageSequence, RandomAccessFile raf, RandomAccessFile rafTemp)
            throws IOException, CannotReadException, CannotWriteException {
        long startAudio = raf.getFilePointer();
        long startAudioWritten = rafTemp.getFilePointer();

        ByteBuffer bb = ByteBuffer.allocate((int) (raf.length() - raf.getFilePointer()));
        ByteBuffer bbTemp = ByteBuffer.allocate((int) (raf.length() - raf.getFilePointer()));

        raf.getChannel().read(bb);
        bb.rewind();

        long bytesToDiscard = 0;
        while (bb.hasRemaining()) {
            OggPageHeader nextPage;
            try {
                nextPage = OggPageHeader.read(bb);
            } catch (CannotReadException cre) {
                bb.position(bb.position() - OggPageHeader.CAPTURE_PATTERN.length);
                if (Utils.readThreeBytesAsChars(bb).equals(AbstractID3v1Tag.TAG)) {
                    bytesToDiscard = bb.remaining() + AbstractID3v1Tag.TAG.length();
                    break;
                } else {
                    throw cre;
                }
            }

            ByteBuffer nextPageHeaderBuffer = ByteBuffer.allocate(
                    nextPage.getRawHeaderData().length + nextPage.getPageLength());
            nextPageHeaderBuffer.order(ByteOrder.LITTLE_ENDIAN);
            nextPageHeaderBuffer.put(nextPage.getRawHeaderData());
            ByteBuffer data = bb.slice();
            data.limit(nextPage.getPageLength());
            nextPageHeaderBuffer.put(data);
            nextPageHeaderBuffer.putInt(OggPageHeader.FIELD_PAGE_SEQUENCE_NO_POS, ++pageSequence);
            calculateChecksumOverPage(nextPageHeaderBuffer);
            bb.position(bb.position() + nextPage.getPageLength());

            nextPageHeaderBuffer.rewind();
            bbTemp.put(nextPageHeaderBuffer);
        }

        bbTemp.flip();
        rafTemp.getChannel().write(bbTemp);

        if ((raf.length() - startAudio) != ((rafTemp.length() + bytesToDiscard) - startAudioWritten)) {
            throw new CannotWriteException("File written counts don't match, file not written:"
                    + "origAudioLength:" + (raf.length() - startAudio)
                    + ":newAudioLength:" + ((rafTemp.length() + bytesToDiscard) - startAudioWritten)
                    + ":bytesDiscarded:" + bytesToDiscard);
        }
    }

    /**
     * Read the sizes of the Opus headers from the file.
     */
    private OpusHeaderSizes readOpusHeaderSizes(RandomAccessFile raf)
            throws CannotReadException, IOException {
        long filePointer = raf.getFilePointer();

        // 1st page = OpusHead
        OggPageHeader pageHeader = OggPageHeader.read(raf);
        long commentHeaderStartPosition = raf.getFilePointer() + pageHeader.getPageLength();
        raf.seek(commentHeaderStartPosition);

        // 2nd page
        pageHeader = OggPageHeader.read(raf);
        commentHeaderStartPosition = raf.getFilePointer()
                - (OggPageHeader.OGG_PAGE_HEADER_FIXED_LENGTH + pageHeader.getSegmentTable().length);

        List<OggPageHeader.PacketStartAndLength> packetList = pageHeader.getPacketList();

        // Check if the first packet is an OpusTags comment header
        byte[] headerCheck = new byte[OpusIdentificationHeader.OPUS_TAGS_MAGIC_LENGTH];
        raf.read(headerCheck);

        int commentHeaderSize = 0;
        List<OggPageHeader.PacketStartAndLength> extraPackets = packetList;
        boolean commentFound = reader.isOpusCommentHeader(headerCheck);

        if (commentFound) {
            // Calculate comment size
            raf.seek(raf.getFilePointer() - headerCheck.length);
            int firstPacketLen = packetList.get(0).getLength();

            commentHeaderSize = firstPacketLen;
            raf.skipBytes(firstPacketLen);

            if (packetList.size() > 1) {
                extraPackets = packetList.subList(1, packetList.size());
            } else {
                extraPackets = List.of();
            }
        } else {
            commentHeaderSize = 0;
            extraPackets = packetList;
        }

        raf.seek(filePointer);
        return new OpusHeaderSizes(commentHeaderStartPosition, commentHeaderSize, extraPackets);
    }

    /**
     * Create segment table for comment data plus extra packets.
     */
    private byte[] createSegmentTable(int commentLength,
                                       List<OggPageHeader.PacketStartAndLength> extraPackets) {
        ByteArrayOutputStream resultBaos = new ByteArrayOutputStream();

        byte[] commentSegments;
        if (extraPackets.isEmpty()) {
            commentSegments = createSegments(commentLength, false);
        } else {
            commentSegments = createSegments(commentLength, true);
        }

        try {
            resultBaos.write(commentSegments);
            for (OggPageHeader.PacketStartAndLength packet : extraPackets) {
                byte[] packetSegments = createSegments(packet.getLength(), false);
                resultBaos.write(packetSegments);
            }
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to create segment table: " + ioe.getMessage());
        }
        return resultBaos.toByteArray();
    }

    /**
     * Create segments for a given data length.
     */
    private byte[] createSegments(int length, boolean quitStream) {
        if (length == 0) {
            return new byte[]{0x00};
        }

        int segmentCount = length / OggPageHeader.MAXIMUM_SEGMENT_SIZE
                + ((length % OggPageHeader.MAXIMUM_SEGMENT_SIZE == 0 && quitStream) ? 1 : 0);
        if (segmentCount == 0) {
            segmentCount = 1;
        }

        byte[] result = new byte[segmentCount];
        int i = 0;
        for (; i < result.length - 1; i++) {
            result[i] = (byte) 0xFF;
        }
        result[result.length - 1] = (byte) (length - (i * OggPageHeader.MAXIMUM_SEGMENT_SIZE));
        return result;
    }

    /**
     * Check if the comment data fits on a single Ogg page.
     */
    private boolean isFitsOnASinglePage(int commentLength,
                                         List<OggPageHeader.PacketStartAndLength> extraPackets) {
        int totalSegments = 0;

        if (commentLength == 0) {
            totalSegments++;
        } else {
            totalSegments = (commentLength / OggPageHeader.MAXIMUM_SEGMENT_SIZE) + 1;
            if (commentLength % OggPageHeader.MAXIMUM_SEGMENT_SIZE == 0) {
                totalSegments++;
            }
        }

        for (OggPageHeader.PacketStartAndLength packet : extraPackets) {
            if (packet.getLength() == 0) {
                totalSegments++;
            } else {
                totalSegments += (packet.getLength() / OggPageHeader.MAXIMUM_SEGMENT_SIZE) + 1;
                if (packet.getLength() % OggPageHeader.MAXIMUM_SEGMENT_SIZE == 0) {
                    totalSegments++;
                }
            }
        }

        return totalSegments <= OggPageHeader.MAXIMUM_NO_OF_SEGMENT_SIZE;
    }

    /**
     * Holds information about the size and position of the Opus comment header.
     */
    private static class OpusHeaderSizes {
        final long commentHeaderStartPosition;
        final int commentHeaderSize;
        final List<OggPageHeader.PacketStartAndLength> extraPacketList;
        final int extraPacketDataSize;

        OpusHeaderSizes(long commentHeaderStartPosition, int commentHeaderSize,
                        List<OggPageHeader.PacketStartAndLength> extraPacketList) {
            this.commentHeaderStartPosition = commentHeaderStartPosition;
            this.commentHeaderSize = commentHeaderSize;
            this.extraPacketList = extraPacketList;
            int size = 0;
            for (OggPageHeader.PacketStartAndLength p : extraPacketList) {
                size += p.getLength();
            }
            this.extraPacketDataSize = size;
        }
    }
}
