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
package org.jaudiotagger.audio.ogg.util;

import org.jaudiotagger.audio.SupportedFileFormat;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.generic.GenericAudioHeader;
import org.jaudiotagger.audio.generic.Utils;
import org.jaudiotagger.logging.ErrorMessage;
import org.jaudiotagger.tag.id3.AbstractID3v2Tag;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Read encoding info from an Opus stream within an Ogg container.
 *
 * Opus always operates at 48 kHz internally. The duration is calculated from
 * the last Ogg page's granule position minus the pre-skip value:
 *   duration = (last_granule_position - pre_skip) / 48000.0
 *
 * Opus has no bitrate fields in its identification header (it's inherently VBR),
 * so we compute the average bitrate from the file size and duration.
 */
public class OpusInfoReader {
    // Logger Object
    public static Logger logger = Logger.getLogger("org.jaudiotagger.audio.ogg.atom");

    /**
     * Read Opus encoding information from the file.
     *
     * @param raf the random access file positioned at the start
     * @return a GenericAudioHeader populated with Opus stream info
     * @throws CannotReadException if the file is not a valid Ogg Opus file
     * @throws IOException         if an I/O error occurs
     */
    public GenericAudioHeader read(RandomAccessFile raf) throws CannotReadException, IOException {
        long start = raf.getFilePointer();
        GenericAudioHeader info = new GenericAudioHeader();
        logger.fine("Started reading Opus info");

        // Check start of file for Ogg pattern
        byte[] b = new byte[OggPageHeader.CAPTURE_PATTERN.length];
        raf.read(b);
        if (!Arrays.equals(b, OggPageHeader.CAPTURE_PATTERN)) {
            raf.seek(0);
            if (AbstractID3v2Tag.isId3Tag(raf)) {
                raf.read(b);
                if (Arrays.equals(b, OggPageHeader.CAPTURE_PATTERN)) {
                    start = raf.getFilePointer() - OggPageHeader.CAPTURE_PATTERN.length;
                }
            } else {
                throw new CannotReadException(ErrorMessage.OGG_HEADER_CANNOT_BE_FOUND.getMsg(new String(b)));
            }
        }

        // Save file length for bitrate calculation
        long fileLength = raf.length();

        OggPageHeader lastPageHeader = OggPageHeader.readLast(raf);
        raf.seek(start);
        double pcmSamplesNumber = lastPageHeader.getAbsoluteGranulePosition();

        if (pcmSamplesNumber == -1) {
            // A value of -1 indicates no packet finished on this page, which should not occur
            throw new CannotReadException(ErrorMessage.OGG_VORBIS_NO_SETUP_BLOCK.getMsg());
        }

        // Read first Ogg page to get the OpusHead (identification header)
        OggPageHeader pageHeader = OggPageHeader.read(raf);
        byte[] opusData = new byte[pageHeader.getPageLength()];

        if (opusData.length < OpusIdentificationHeader.OPUS_HEAD_MIN_SIZE) {
            throw new CannotReadException("Invalid Opus identification header for this Ogg File");
        }
        raf.read(opusData);
        OpusIdentificationHeader opusIdentificationHeader = new OpusIdentificationHeader(opusData);

        // Map to generic encoding info
        // Duration: (total_samples - pre_skip) / 48000.0
        int preSkip = opusIdentificationHeader.getPreSkip();
        double playableSamples = pcmSamplesNumber - preSkip;
        if (playableSamples < 0) {
            playableSamples = 0;
        }
        double duration = playableSamples / OpusIdentificationHeader.OPUS_INTERNAL_SAMPLE_RATE;
        info.setPreciseLength((float) duration);

        info.setChannelNumber(opusIdentificationHeader.getChannelNumber());
        info.setSamplingRate(OpusIdentificationHeader.OPUS_INTERNAL_SAMPLE_RATE);
        info.setEncodingType(opusIdentificationHeader.getEncodingType());
        info.setFormat("Opus");

        // Opus uses variable bitrate — compute average from file size and duration
        info.setBitRate(computeBitrate((int) duration, fileLength));
        info.setVariableBitRate(true);

        // Opus always decodes to 16-bit PCM (or higher with dithering)
        info.setBitsPerSample(16);

        logger.fine("Opus Info: channels=" + info.getChannelNumber()
                + ", sampleRate=" + info.getSampleRate()
                + ", duration=" + info.getPreciseTrackLength()
                + ", bitRate=" + info.getBitRate());

        return info;
    }

    private int computeBitrate(int lengthInSeconds, long sizeInBytes) {
        // Protect against audio less than 0.5 seconds that can be rounded to zero
        if (lengthInSeconds == 0) {
            lengthInSeconds = 1;
        }
        return (int) ((sizeInBytes / Utils.KILOBYTE_MULTIPLIER) * Utils.BITS_IN_BYTE_MULTIPLIER / lengthInSeconds);
    }
}
