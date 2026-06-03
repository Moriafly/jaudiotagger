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

import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * Opus Identification Header
 *
 * From RFC 7845 - Ogg Encapsulation for the Opus Audio Codec
 *
 * The OpusHead (identification header) is the raw page data of the first Ogg page.
 * Unlike Vorbis, Opus does NOT use a separate packet type byte — the magic string
 * "OpusHead" itself identifies the packet type.
 *
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |      'O'      |      'p'      |      'u'      |      's'      |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |      'H'      |      'e'      |      'a'      |      'd'      |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  Version = 1  | Channel Count |           Pre-skip            |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                     Input Sample Rate (Hz)                    |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |   Output Gain (Q7.8 in dB)    | Mapping Family|               |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+               :
 * :                                                               :
 * :               Optional Channel Mapping Table...               :
 * :                                                               :
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *
 * Minimum size is 19 bytes: magic(8) + version(1) + channels(1) + pre-skip(2)
 * + sample rate(4) + gain(2) + mapping family(1).
 */
@SuppressWarnings("unused")
public class OpusIdentificationHeader {
    // Logger Object
    public static Logger logger = Logger.getLogger("org.jaudiotagger.audio.ogg.atom");

    /** Magic signature for the OpusHead packet */
    public static final String OPUS_HEAD_MAGIC = "OpusHead";
    public static final byte[] OPUS_HEAD_MAGIC_AS_BYTES = {'O', 'p', 'u', 's', 'H', 'e', 'a', 'd'};
    public static final int OPUS_HEAD_MAGIC_LENGTH = 8;

    /** Magic signature for the OpusTags packet */
    public static final String OPUS_TAGS_MAGIC = "OpusTags";
    public static final byte[] OPUS_TAGS_MAGIC_AS_BYTES = {'O', 'p', 'u', 's', 'T', 'a', 'g', 's'};
    public static final int OPUS_TAGS_MAGIC_LENGTH = 8;

    /** Minimum size of a valid OpusHead packet: magic(8) + version(1) + channels(1) + pre-skip(2) + sample rate(4) + gain(2) + mapping family(1) */
    public static final int OPUS_HEAD_MIN_SIZE = OPUS_HEAD_MAGIC_LENGTH + 1 + 1 + 2 + 4 + 2 + 1;

    // Field positions within the raw page data (magic "OpusHead" starts at offset 0)
    public static final int FIELD_OPUS_VERSION_POS = OPUS_HEAD_MAGIC_LENGTH;
    public static final int FIELD_OPUS_CHANNELS_POS = FIELD_OPUS_VERSION_POS + 1;
    public static final int FIELD_OPUS_PRE_SKIP_POS = FIELD_OPUS_CHANNELS_POS + 1;
    public static final int FIELD_OPUS_INPUT_SAMPLE_RATE_POS = FIELD_OPUS_PRE_SKIP_POS + 2;
    public static final int FIELD_OPUS_OUTPUT_GAIN_POS = FIELD_OPUS_INPUT_SAMPLE_RATE_POS + 4;
    public static final int FIELD_OPUS_MAPPING_FAMILY_POS = FIELD_OPUS_OUTPUT_GAIN_POS + 2;

    /** Opus always operates at 48 kHz internally */
    public static final int OPUS_INTERNAL_SAMPLE_RATE = 48000;

    /** Duration of seek pre-roll in milliseconds (80ms per RFC 7845) */
    public static final int SEEK_PREROLL_MS = 80;

    // Parsed fields
    private int version;
    private int channelCount;
    private int preSkip;
    private int inputSampleRate;
    private int outputGain; // Q7.8 fixed-point
    private int channelMappingFamily;
    private int streamCount;
    private int coupledCount;
    private byte[] channelMapping;
    private boolean isValid = false;

    public OpusIdentificationHeader(byte[] opusData) {
        decodeHeader(opusData);
    }

    /**
     * Decode the OpusHead header from raw page data.
     *
     * @param b raw page data (starts directly with "OpusHead" magic, no packet type byte)
     */
    public void decodeHeader(byte[] b) {
        if (b.length < OPUS_HEAD_MIN_SIZE) {
            logger.warning("OpusHead packet too short: " + b.length + " bytes, minimum is " + OPUS_HEAD_MIN_SIZE);
            return;
        }

        String magic = new String(b, 0, OPUS_HEAD_MAGIC_LENGTH, StandardCharsets.ISO_8859_1);
        if (!magic.equals(OPUS_HEAD_MAGIC)) {
            logger.warning("Not a valid OpusHead header: magic=" + magic);
            return;
        }

        this.version = u(b[FIELD_OPUS_VERSION_POS]);
        logger.fine("Opus version: " + version);

        this.channelCount = u(b[FIELD_OPUS_CHANNELS_POS]);
        logger.fine("Channel count: " + channelCount);

        this.preSkip = u(b[FIELD_OPUS_PRE_SKIP_POS]) | (u(b[FIELD_OPUS_PRE_SKIP_POS + 1]) << 8);
        logger.fine("Pre-skip: " + preSkip);

        this.inputSampleRate = u(b[FIELD_OPUS_INPUT_SAMPLE_RATE_POS])
                | (u(b[FIELD_OPUS_INPUT_SAMPLE_RATE_POS + 1]) << 8)
                | (u(b[FIELD_OPUS_INPUT_SAMPLE_RATE_POS + 2]) << 16)
                | (u(b[FIELD_OPUS_INPUT_SAMPLE_RATE_POS + 3]) << 24);
        logger.fine("Input sample rate: " + inputSampleRate);

        // Output gain is a signed 16-bit integer in Q7.8 fixed-point format
        this.outputGain = (short) (u(b[FIELD_OPUS_OUTPUT_GAIN_POS]) | (u(b[FIELD_OPUS_OUTPUT_GAIN_POS + 1]) << 8));
        logger.fine("Output gain (raw): " + outputGain);

        this.channelMappingFamily = u(b[FIELD_OPUS_MAPPING_FAMILY_POS]);
        logger.fine("Channel mapping family: " + channelMappingFamily);

        // Parse channel mapping table if present
        if (channelMappingFamily > 0 && b.length > FIELD_OPUS_MAPPING_FAMILY_POS + 2) {
            int pos = FIELD_OPUS_MAPPING_FAMILY_POS + 1;
            this.streamCount = u(b[pos]);
            logger.fine("Stream count: " + streamCount);
            pos++;

            this.coupledCount = u(b[pos]);
            logger.fine("Coupled count: " + coupledCount);
            pos++;

            int mappingLength = channelCount;
            if (pos + mappingLength <= b.length) {
                this.channelMapping = new byte[mappingLength];
                System.arraycopy(b, pos, this.channelMapping, 0, mappingLength);
            }
        }

        // For channel mapping family 0:
        //   1 channel = mono (1 stream, 0 coupled)
        //   2 channels = stereo (1 stream, 1 coupled)
        if (channelMappingFamily == 0) {
            this.streamCount = (channelCount <= 2) ? 1 : 0;
            this.coupledCount = (channelCount == 2) ? 1 : 0;
        }

        // Validate version: the upper 4 bits must be 0 (version 0-15 allowed per spec)
        if ((version & 0xF0) == 0) {
            isValid = true;
        }
    }

    public String getEncodingType() {
        return "Opus";
    }

    public int getChannelNumber() {
        return channelCount;
    }

    public int getPreSkip() {
        return preSkip;
    }

    public int getInputSampleRate() {
        return inputSampleRate;
    }

    public int getOutputGain() {
        return outputGain;
    }

    public double getOutputGainDB() {
        return outputGain / 256.0;
    }

    public int getChannelMappingFamily() {
        return channelMappingFamily;
    }

    public int getStreamCount() {
        return streamCount;
    }

    public int getCoupledCount() {
        return coupledCount;
    }

    public byte[] getChannelMapping() {
        return channelMapping;
    }

    public boolean isValid() {
        return isValid;
    }

    public int getVersion() {
        return version;
    }

    public int getSamplingRate() {
        return OPUS_INTERNAL_SAMPLE_RATE;
    }

    /**
     * Check if the given page data looks like a valid OpusHead identification header.
     *
     * @param headerData raw page data (starts directly with "OpusHead" magic)
     * @return true if this appears to be an OpusHead identification header
     */
    public static boolean isOpusIdentificationHeader(byte[] headerData) {
        if (headerData.length < OPUS_HEAD_MAGIC_LENGTH) {
            return false;
        }
        String magic = new String(headerData, 0, OPUS_HEAD_MAGIC_LENGTH, StandardCharsets.ISO_8859_1);
        return OPUS_HEAD_MAGIC.equals(magic);
    }

    private int u(int i) {
        return i & 0xFF;
    }
}
