package org.jaudiotagger.audio.wav.chunk;

import com.moriafly.jaudiotagger.JaudiotaggerFlags;
import org.jaudiotagger.audio.generic.Utils;
import org.jaudiotagger.audio.iff.IffHeaderChunk;
import org.jaudiotagger.tag.FieldDataInvalidException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagOptionSingleton;
import org.jaudiotagger.tag.wav.WavInfoTag;
import org.jaudiotagger.tag.wav.WavTag;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stores basic only metadata but only exists as part of a LIST chunk, doesn't have its own size field
 * instead contains a number of name,size, value tuples. So for this reason we do not subclass the Chunk class
 */
public class WavInfoChunk {
    public static Logger logger = Logger.getLogger("org.jaudiotagger.audio.wav.WavInfoChunk");

    private static final Charset LOSSLESS_FALLBACK_CHARSET = StandardCharsets.ISO_8859_1;

    private final WavInfoTag wavInfoTag;
    private final String loggingName;

    public WavInfoChunk(WavTag tag, String loggingName) {
        this.loggingName = loggingName;
        wavInfoTag = new WavInfoTag();
        tag.setInfoTag(wavInfoTag);
    }

    /**
     * Read Info chunk.
     *
     * @param chunkData INFO records in little-endian byte order
     * @return {@code true} when the complete chunk was structurally valid
     */
    public boolean readChunks(ByteBuffer chunkData) {
        List<InfoField> fields = readRawFields(chunkData);
        if (fields == null) {
            return false;
        }

        TagOptionSingleton options = TagOptionSingleton.getInstance();
        Charset defaultCharset = determineDefaultCharset(fields, options);
        EnumSet<FieldKey> overrideFieldKeys = options.getOverrideCharsetFields();

        for (InfoField field : fields) {
            Charset charset = defaultCharset;
            int bomLength = 0;

            if (shouldOverrideCharset(field.identifier, options, overrideFieldKeys)) {
                charset = options.getOverrideCharset();
            } else {
                Bom bom = findBom(field.data);
                if (bom != null) {
                    charset = bom.charset;
                    bomLength = bom.length;
                }
            }

            String value = decodeField(field, charset, bomLength);

            if (field.identifier != null && field.identifier.getFieldKey() != null) {
                try {
                    wavInfoTag.setField(field.identifier.getFieldKey(), value);
                } catch (FieldDataInvalidException fdie) {
                    logger.log(Level.SEVERE, loggingName + fdie.getMessage(), fdie);
                }
            } else {
                wavInfoTag.addUnRecognizedField(field.id, value);
            }
        }
        return true;
    }

    /**
     * Parse the INFO records before decoding any text. This makes malformed
     * chunks transactional: no partially decoded tag is exposed on failure.
     */
    private List<InfoField> readRawFields(ByteBuffer chunkData) {
        List<InfoField> fields = new ArrayList<>();

        while (chunkData.hasRemaining()) {
            if (chunkData.remaining() < IffHeaderChunk.TYPE_LENGTH + IffHeaderChunk.SIZE_LENGTH) {
                if (isPadding(chunkData)) {
                    return fields;
                }
                logger.severe(loggingName + "LIST/INFO has a truncated field header of "
                        + chunkData.remaining() + " byte(s)");
                return null;
            }

            String id = Utils.readFourBytesAsChars(chunkData);
            long size = Integer.toUnsignedLong(chunkData.getInt());

            if (isPaddingIdentifier(id)) {
                return fields;
            }

            // #655: numeric characters are accepted for compatibility with
            // non-standard writers, but INFO identifiers remain ASCII only.
            if (!isValidIdentifier(id)) {
                logger.severe(loggingName + "LIST/INFO appears corrupt, invalid identifier:"
                        + id + ":" + size);
                return null;
            }

            if (size > chunkData.remaining()) {
                logger.severe(loggingName + "LIST/INFO field " + id + " declares " + size
                        + " byte(s), only " + chunkData.remaining() + " remain");
                return null;
            }

            byte[] data = new byte[(int) size];
            chunkData.get(data);
            fields.add(new InfoField(id, data, WavInfoIdentifier.getByCode(id)));

            // Every INFO record is aligned to an even byte boundary.
            if ((size & 1) != 0) {
                if (!chunkData.hasRemaining()) {
                    logger.severe(loggingName + "LIST/INFO field " + id + " is missing its alignment byte");
                    return null;
                }
                chunkData.get();
            }
        }
        return fields;
    }

    /**
     * INFO has no charset marker at chunk level. UTF-8 is accepted only after
     * strict validation of every non-overridden, non-BOM field. If one field is
     * invalid, the configured legacy fallback is applied consistently to the
     * whole chunk. Without a configured fallback ISO-8859-1 is used to preserve
     * every byte for round-tripping instead of inserting replacement characters.
     */
    private Charset determineDefaultCharset(List<InfoField> fields, TagOptionSingleton options) {
        EnumSet<FieldKey> overrideFieldKeys = options.getOverrideCharsetFields();

        for (InfoField field : fields) {
            if (shouldOverrideCharset(field.identifier, options, overrideFieldKeys)
                    || findBom(field.data) != null
                    || isAscii(field.data)) {
                continue;
            }

            if (!canDecode(field.data, StandardCharsets.UTF_8, 0)) {
                Charset fallback = JaudiotaggerFlags.wavInfoFallbackCharset;
                if (fallback != null && canDecodeAll(fields, fallback, options, overrideFieldKeys)) {
                    logger.fine(loggingName + "LIST/INFO is not valid UTF-8; using configured fallback "
                            + fallback.displayName());
                    return fallback;
                }

                if (fallback != null) {
                    logger.warning(loggingName + "LIST/INFO is invalid in both UTF-8 and configured fallback "
                            + fallback.displayName() + "; preserving raw bytes as ISO-8859-1");
                } else {
                    logger.warning(loggingName + "LIST/INFO is not valid UTF-8 and no legacy fallback is configured; "
                            + "preserving raw bytes as ISO-8859-1");
                }
                return LOSSLESS_FALLBACK_CHARSET;
            }
        }
        return StandardCharsets.UTF_8;
    }

    private boolean canDecodeAll(List<InfoField> fields, Charset charset, TagOptionSingleton options,
                                 EnumSet<FieldKey> overrideFieldKeys) {
        for (InfoField field : fields) {
            if (shouldOverrideCharset(field.identifier, options, overrideFieldKeys)
                    || findBom(field.data) != null
                    || isAscii(field.data)) {
                continue;
            }
            if (!canDecode(field.data, charset, 0)) {
                return false;
            }
        }
        return true;
    }

    private boolean shouldOverrideCharset(WavInfoIdentifier identifier, TagOptionSingleton options,
                                          EnumSet<FieldKey> overrideFieldKeys) {
        if (!options.isOverrideCharsetForInfo() || options.getOverrideCharset() == null) {
            return false;
        }

        // An empty field set means a chunk-wide override. This is necessary for
        // unknown vendor fields, which have no FieldKey to select individually.
        return overrideFieldKeys.isEmpty()
                || (identifier != null
                && identifier.getFieldKey() != null
                && overrideFieldKeys.contains(identifier.getFieldKey()));
    }

    private String decodeField(InfoField field, Charset charset, int bomLength) {
        try {
            String value = decode(field.data, charset, bomLength);
            logger.config(loggingName + "Result:" + field.id + ":" + field.data.length + ":" + value + ":");
            return value;
        } catch (CharacterCodingException e) {
            logger.log(Level.WARNING, loggingName + "LIST/INFO field " + field.id
                    + " cannot be decoded as " + charset.displayName()
                    + "; preserving raw bytes as ISO-8859-1", e);
            try {
                return decode(field.data, LOSSLESS_FALLBACK_CHARSET, 0);
            } catch (CharacterCodingException impossible) {
                throw new IllegalStateException("ISO-8859-1 must decode every byte", impossible);
            }
        }
    }

    private static boolean canDecode(byte[] data, Charset charset, int bomLength) {
        try {
            decode(data, charset, bomLength);
            return true;
        } catch (CharacterCodingException e) {
            return false;
        }
    }

    private static String decode(byte[] data, Charset charset, int bomLength) throws CharacterCodingException {
        int end = contentEnd(data, bomLength, charset);
        ByteBuffer input = ByteBuffer.wrap(data, bomLength, end - bomLength);
        return charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(input)
                .toString();
    }

    private static int contentEnd(byte[] data, int start, Charset charset) {
        int end = data.length;
        int terminatorWidth = charset.name().startsWith("UTF-16") ? 2 : 1;

        while (end - start >= terminatorWidth) {
            boolean terminator = true;
            for (int i = 1; i <= terminatorWidth; i++) {
                if (data[end - i] != 0) {
                    terminator = false;
                    break;
                }
            }
            if (!terminator) {
                break;
            }
            end -= terminatorWidth;
        }
        return end;
    }

    private static Bom findBom(byte[] data) {
        if (data.length >= 3
                && (data[0] & 0xff) == 0xef
                && (data[1] & 0xff) == 0xbb
                && (data[2] & 0xff) == 0xbf) {
            return new Bom(StandardCharsets.UTF_8, 3);
        }
        if (data.length >= 2 && (data[0] & 0xff) == 0xfe && (data[1] & 0xff) == 0xff) {
            return new Bom(StandardCharsets.UTF_16BE, 2);
        }
        if (data.length >= 2 && (data[0] & 0xff) == 0xff && (data[1] & 0xff) == 0xfe) {
            return new Bom(StandardCharsets.UTF_16LE, 2);
        }
        return null;
    }

    private static boolean isAscii(byte[] data) {
        for (byte value : data) {
            if ((value & 0x80) != 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidIdentifier(String id) {
        if (id.length() != IffHeaderChunk.TYPE_LENGTH) {
            return false;
        }
        for (int i = 0; i < id.length(); i++) {
            char character = id.charAt(i);
            if (!((character >= 'A' && character <= 'Z')
                    || (character >= 'a' && character <= 'z')
                    || (character >= '0' && character <= '9'))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isPaddingIdentifier(String id) {
        for (int i = 0; i < id.length(); i++) {
            char character = id.charAt(i);
            if (character != 0 && character != ' ') {
                return false;
            }
        }
        return true;
    }

    private static boolean isPadding(ByteBuffer data) {
        ByteBuffer remaining = data.slice();
        while (remaining.hasRemaining()) {
            byte value = remaining.get();
            if (value != 0 && value != ' ') {
                return false;
            }
        }
        data.position(data.limit());
        return true;
    }

    private static final class InfoField {
        private final String id;
        private final byte[] data;
        private final WavInfoIdentifier identifier;

        private InfoField(String id, byte[] data, WavInfoIdentifier identifier) {
            this.id = id;
            this.data = data;
            this.identifier = identifier;
        }
    }

    private static final class Bom {
        private final Charset charset;
        private final int length;

        private Bom(Charset charset, int length) {
            this.charset = charset;
            this.length = length;
        }
    }
}
