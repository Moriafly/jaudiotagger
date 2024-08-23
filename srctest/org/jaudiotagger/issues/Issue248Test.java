package org.jaudiotagger.issues;

import org.jaudiotagger.AbstractTestCase;
import org.jaudiotagger.audio.mp3.MP3File;

import java.io.File;


public class Issue248Test extends AbstractTestCase
{
    public static int countExceptions =0;
    public void testMultiThreadedMP3HeaderAccess() throws Exception
    {
        final File testFile = AbstractTestCase.copyAudioToTmp("testV1vbrOld0.mp3");
        new MP3File(testFile);
        final Thread[] threads = new Thread[1000];
        for(int i = 0; i < 1000; i++)
        {
            threads[i] = new Thread(new Runnable()
            {
                public void run()
                {
                    try
                    {
                         //System.out.println("Output is"+mp3File.getMP3AudioHeader().getTrackLengthAsString());
                    }
                    catch (RuntimeException e)
                    {
                        e.printStackTrace();
                        countExceptions++;
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        countExceptions++;
                    }
                }
            });
        }

        for(int i = 0; i < 1000; i++)
        {
            threads[i].start();
        }

        assertEquals(0, countExceptions);
    }
}
