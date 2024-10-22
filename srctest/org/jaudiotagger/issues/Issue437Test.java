package org.jaudiotagger.issues;

import org.jaudiotagger.AbstractTestCase;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;

import java.io.File;

/**
 * Adding 'getBitsPerSample()' accessor to AudioHeader
 */
public class Issue437Test extends AbstractTestCase
{
    public void testGetBitsPerSampleFlac()
    {
        Throwable e = null;
        try
        {
            File testFile = AbstractTestCase.copyAudioToTmp("test.flac");
            AudioFile af = AudioFileIO.read(testFile);
            assertEquals(16,af.getAudioHeader().getBitsPerSample());
        }
        catch(Exception ex)
        {
            e=ex;
        }
        assertNull(e);
    }

    public void testGetBitsPerSampleMp4()
    {
        Throwable e = null;
        try
        {
            File testFile = AbstractTestCase.copyAudioToTmp("test.m4a");
            AudioFile af = AudioFileIO.read(testFile);
            assertEquals(16,af.getAudioHeader().getBitsPerSample());
        }
        catch(Exception ex)
        {
            e=ex;
        }
        assertNull(e);
    }

    public void testGetBitsPerSampleOgg()
    {
        Throwable e = null;
        try
        {
            File testFile = AbstractTestCase.copyAudioToTmp("test.ogg");
            AudioFile af = AudioFileIO.read(testFile);
            assertEquals(16,af.getAudioHeader().getBitsPerSample());
        }
        catch(Exception ex)
        {
            e=ex;
        }
        assertNull(e);
    }

    public void testGetBitsPerSampleWma()
    {
        Throwable e = null;
        try
        {
            File testFile = AbstractTestCase.copyAudioToTmp("test1.wma");
            AudioFile af = AudioFileIO.read(testFile);
            assertEquals(16,af.getAudioHeader().getBitsPerSample());
        }
        catch(Exception ex)
        {
            e=ex;
        }
        assertNull(e);
    }

    public void testGetBitsPerSampleMp3()
    {
        Throwable e = null;
        try
        {
            File testFile = AbstractTestCase.copyAudioToTmp("testV1.mp3", new File("testGetBitsPerSampleMp3.mp3"));
            AudioFile af = AudioFileIO.read(testFile);
            assertEquals(16,af.getAudioHeader().getBitsPerSample());
        }
        catch(Exception ex)
        {
            e=ex;
        }
        assertNull(e);
    }
}
