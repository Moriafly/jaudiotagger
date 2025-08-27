package org.jaudiotagger.audio.dff;

import org.jaudiotagger.audio.exceptions.InvalidChunkException;
import org.jaudiotagger.audio.generic.Utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Base Chunk for all chunks in the dff FRM8 Chunk.
 */
public class BaseChunk {
    public static final int ID_LENGTH = 4;
    private Long chunkSize;
    private Long chunkStart;

    protected BaseChunk() {}

    protected void readDataChunk(FileChannel fc) throws IOException {
        readDataChunkHeader(fc);
        skipToChunkEnd(fc);
    }

    protected final void readDataChunkHeader(FileChannel fc) throws IOException {
        ByteBuffer chunkHeader = Utils.readFileDataIntoBufferLE(fc, 8);
        chunkSize = Long.reverseBytes(chunkHeader.getLong());
        chunkStart = fc.position();
    }

    protected final void skipToChunkEnd(FileChannel fc) throws IOException {
        long skip = (this.getChunkEnd() - fc.position());

        if (skip > 0) {
            // Read audio data
            Utils.readFileDataIntoBufferLE(fc, (int) skip);
        }
    }

    /**
     * @return the chunk Start position
     */
    public Long getChunkStart() {
        return chunkStart;
    }

    /**
     * @return the chunkSize
     */
    public Long getChunkSize() {
        return chunkSize;
    }

    /**
     * @return the chunk End position.
     */
    public Long getChunkEnd() {
        return chunkStart + chunkSize;
    }

    public static BaseChunk readIdChunk(ByteBuffer dataBuffer) throws InvalidChunkException {

        String type = Utils.readFourBytesAsChars(dataBuffer);
        //System.out.println("BaseChunk.type: "+type);

        if (DffChunkType.FS.getCode().equals(type)) {
            return new FsChunk();
        } else if (DffChunkType.CHNL.getCode().equals(type)) {
            return new ChnlChunk();
        } else if (DffChunkType.CMPR.getCode().equals(type)) {
            return new CmprChunk();
        } else if (DffChunkType.END.getCode().equals(type) || DffChunkType.DSD.getCode().equals(type)) {
            return new EndChunk();
        } else if (DffChunkType.DST.getCode().equals(type)) {
            return new DstChunk();
        } else if (DffChunkType.FRTE.getCode().equals(type)) {
            return new FrteChunk();
        } else if (DffChunkType.ID3.getCode().equals(type)) {
            return new BaseChunk();
        } else if (DffChunkType.ABSS.getCode().equals(type)) {
            return new BaseChunk();
        } else if (DffChunkType.LSCO.getCode().equals(type)) {
            return new BaseChunk();
        } else {
            throw new InvalidChunkException(type + " is not recognized as a valid DFF chunk");
        }
    }
}
