package org.jaudiotagger.audio.asf;

import org.jaudiotagger.audio.asf.data.AsfHeader;
import org.jaudiotagger.audio.asf.util.TagConverter;
import org.jaudiotagger.audio.asf.util.Utils;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.tag.asf.AsfTag;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.logging.Logger;

/**
 * Reads Tags (Metadata) from an ASF file.
 */
public class AsfTagReader {

    private static final Logger LOGGER = Logger.getLogger("org.jaudiotagger.audio.asf");

    /**
     * Reads the Tag from a FileChannel.
     * This will perform a fresh parse of the ASF Header structure.
     */
    public AsfTag read(FileChannel fc, String fileName) throws CannotReadException, IOException {
        AsfHeader header = Utils.readAsfHeader(fc, fileName);
        return build(header);
    }

    /**
     * Builds the AsfTag from an already parsed AsfHeader.
     */
    public AsfTag build(final AsfHeader header) {
        return TagConverter.createTagOf(header);
    }
}