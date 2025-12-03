package org.jaudiotagger.audio.asf;

import org.jaudiotagger.audio.SupportedFileFormat;
import org.jaudiotagger.audio.asf.data.AsfHeader;
import org.jaudiotagger.audio.asf.data.AudioStreamChunk;
import org.jaudiotagger.audio.asf.data.MetadataContainer;
import org.jaudiotagger.audio.asf.data.MetadataDescriptor;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.generic.GenericAudioHeader;
import org.jaudiotagger.audio.asf.util.Utils;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.logging.Logger;

/**
 * Reads technical audio information (Bitrate, Duration, etc.) from an ASF file.
 */
public class AsfInfoReader {
    private static final Logger LOGGER = Logger.getLogger("org.jaudiotagger.audio.asf");

    /**
     * Reads audio header from a FileChannel.
     * This will perform a fresh parse of the ASF Header structure.
     */
    public GenericAudioHeader read(FileChannel fc, String fileName) throws CannotReadException, IOException {
        AsfHeader header = Utils.readAsfHeader(fc, fileName);
        return build(header);
    }

    /**
     * Builds the GenericAudioHeader from an already parsed AsfHeader.
     * Use this if you have already parsed the structure to avoid I/O overhead.
     */
    public GenericAudioHeader build(final AsfHeader header) throws CannotReadException {
        final GenericAudioHeader info = new GenericAudioHeader();

        if (header.getFileHeader() == null) {
            throw new CannotReadException("Invalid ASF/WMA file. File header object not available.");
        }
        if (header.getAudioStreamChunk() == null) {
            throw new CannotReadException("Invalid ASF/WMA file. No audio stream contained.");
        }

        info.setBitRate(header.getAudioStreamChunk().getKbps());
        info.setChannelNumber((int) header.getAudioStreamChunk().getChannelCount());
        info.setFormat(SupportedFileFormat.WMA.getDisplayName());
        info.setEncodingType("ASF (audio): " + header.getAudioStreamChunk().getCodecDescription());
        info.setLossless(header.getAudioStreamChunk().getCompressionFormat() == AudioStreamChunk.WMA_LOSSLESS);
        info.setPreciseLength(header.getFileHeader().getPreciseDuration());
        info.setSamplingRate((int) header.getAudioStreamChunk().getSamplingRate());
        info.setVariableBitRate(determineVariableBitrate(header));
        info.setBitsPerSample(header.getAudioStreamChunk().getBitsPerSample());

        return info;
    }

    private boolean determineVariableBitrate(final AsfHeader header) {
        assert header != null;
        boolean result = false;
        final MetadataContainer extDesc = header.findExtendedContentDescription();
        if (extDesc != null) {
            final List<MetadataDescriptor> descriptors = extDesc.getDescriptorsByName("IsVBR");
            if (descriptors != null && !descriptors.isEmpty()) {
                result = Boolean.TRUE.toString().equals(descriptors.get(0).getString());
            }
        }
        return result;
    }
}
