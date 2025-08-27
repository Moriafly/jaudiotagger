package org.jaudiotagger.audio.dff;

import org.jaudiotagger.audio.generic.Utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * FS Chunk. Retrive samplerate.
 */
public class FsChunk extends BaseChunk
{
    private int sampleRate;

    public FsChunk() {super();}

    @Override
    public void readDataChunk(FileChannel fc) throws IOException {
        readDataChunkHeader(fc);

        ByteBuffer audioData = Utils.readFileDataIntoBufferLE(fc, 4);
        sampleRate = Integer.reverseBytes(audioData.getInt());

        skipToChunkEnd(fc);
    }

    /**
     * @return the sampleRate
     */
    public int getSampleRate()
    {
        return sampleRate;
    }
}
