/*
 * Entagged Audio Tag library
 * Copyright (c) 2003-2005 Raphaël Slinckx <raphael@slinckx.net>
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
 * Read encoding info, only implemented for vorbis streams
 */
public class OggInfoReader {
    // Logger Object
    public static Logger logger = Logger.getLogger("org.jaudiotagger.audio.ogg.atom");

    public GenericAudioHeader read(RandomAccessFile raf) throws CannotReadException, IOException {
        long start = raf.getFilePointer();
        GenericAudioHeader info = new GenericAudioHeader();
        logger.fine("Started");
        //Check start of file does it have Ogg pattern
        byte[] b = new byte[OggPageHeader.CAPTURE_PATTERN.length];
        raf.read(b);
        if (!(Arrays.equals(b, OggPageHeader.CAPTURE_PATTERN))) {
            raf.seek(0);
            if (AbstractID3v2Tag.isId3Tag(raf)) {
                raf.read(b);
                if ((Arrays.equals(b, OggPageHeader.CAPTURE_PATTERN))) {
                    start = raf.getFilePointer();
                }
            } else {
                throw new CannotReadException(ErrorMessage.OGG_HEADER_CANNOT_BE_FOUND.getMsg(new String(b)));
            }
        }

        OggPageHeader lastPageHeader = OggPageHeader.readLast(raf);
        raf.seek(0);
        double pcmSamplesNumber = lastPageHeader.getAbsoluteGranulePosition();

        if (pcmSamplesNumber == -1) {
            //According to spec a value of -1 indicates no packet finished on this page, this should not occur
            throw new CannotReadException(ErrorMessage.OGG_VORBIS_NO_SETUP_BLOCK.getMsg());
        }

        //1st page = Identification Header
        OggPageHeader pageHeader = OggPageHeader.read(raf);
        byte[] vorbisData = new byte[pageHeader.getPageLength()];

        if (vorbisData.length < OggPageHeader.OGG_PAGE_HEADER_FIXED_LENGTH) {
            throw new CannotReadException("Invalid Identification header for this Ogg File");
        }
        raf.read(vorbisData);
        VorbisIdentificationHeader vorbisIdentificationHeader = new VorbisIdentificationHeader(vorbisData);

        //Map to generic encodingInfo
        info.setPreciseLength((float) (pcmSamplesNumber / vorbisIdentificationHeader.getSamplingRate()));
        info.setChannelNumber(vorbisIdentificationHeader.getChannelNumber());
        info.setSamplingRate(vorbisIdentificationHeader.getSamplingRate());
        info.setEncodingType(vorbisIdentificationHeader.getEncodingType());
        info.setFormat(SupportedFileFormat.OGG.getDisplayName());

        //According to Wikipedia Vorbis Page, Vorbis only works on 16bits 44khz 
        info.setBitsPerSample(16);

        //TODO this calculation should be done within identification header
        if (vorbisIdentificationHeader.getNominalBitrate() != 0 && vorbisIdentificationHeader.getMaxBitrate() == vorbisIdentificationHeader.getNominalBitrate() && vorbisIdentificationHeader.getMinBitrate() == vorbisIdentificationHeader.getNominalBitrate()) {
            //CBR (in kbps)
            info.setBitRate(vorbisIdentificationHeader.getNominalBitrate() / 1000);
            info.setVariableBitRate(false);
        } else if (vorbisIdentificationHeader.getNominalBitrate() != 0 && vorbisIdentificationHeader.getMaxBitrate() == 0 && vorbisIdentificationHeader.getMinBitrate() == 0) {
            //Average vbr (in kpbs)
            info.setBitRate(vorbisIdentificationHeader.getNominalBitrate() / 1000);
            info.setVariableBitRate(true);
        } else {
            //TODO need to remove comment from raf.getLength()
            info.setBitRate(computeBitrate(info.getTrackLength(), raf.length()));
            info.setVariableBitRate(true);
        }
        return info;
    }

    private int computeBitrate(int length, long size) {
        //Protect against audio less than 0.5 seconds that can be rounded to zero causing Arithmetic Exception
        if (length == 0) {
            length = 1;
        }
        return (int) ((size / Utils.KILOBYTE_MULTIPLIER) * Utils.BITS_IN_BYTE_MULTIPLIER / length);
    }
}

