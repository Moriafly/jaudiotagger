package org.jaudiotagger.issues;

import org.jaudiotagger.AbstractTestCase;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Issue321.
 */
public class Issue321Test extends AbstractTestCase {

    public void testReadingMP4WithExtraByte() throws Exception
    {
        File testFile = createMP4WithExtraByte();
        if (testFile == null) return;

        AudioFile af = AudioFileIO.read(testFile);
        assertNotNull(af.getTag());
        System.out.println(af.getTag());

        AudioFileIO.write(af);
    }

    public void testReadingMP4WithExtraByteAndWrite() throws Exception
    {
        File testFile = createMP4WithExtraByte();
        if (testFile == null) return;

        AudioFile af = AudioFileIO.read(testFile);
        assertNotNull(af.getTag());
        final String afString = af.getTag().toString();

        // write
        AudioFileIO.write(af);

        AudioFile rereadAF = AudioFileIO.read(testFile);
        final String rereadAfString = rereadAF.getTag().toString();
        assertNotNull(rereadAfString);

        // ensure same
        assertEquals(afString, rereadAfString);
    }

    public void testReadingMP4WithExtraByteAndModifiedWrite() throws Exception
    {
        File testFile = createMP4WithExtraByte();
        if (testFile == null) return;

        final long originalLength = testFile.length();
        AudioFile af = AudioFileIO.read(testFile);
        assertNotNull(af.getTag());
        // add long fields
        af.getTag().setField(FieldKey.ALBUM, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        af.getTag().setField(FieldKey.ARTIST, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        af.getTag().setField(FieldKey.COMPOSER, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        af.getTag().setField(FieldKey.TITLE, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        af.getTag().setField(FieldKey.WORK, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        af.getTag().setField(FieldKey.COMMENT, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");

        // write
        AudioFileIO.write(af);

        AudioFile rereadAF = AudioFileIO.read(testFile);
        final String rereadAfString = rereadAF.getTag().toString();
        assertNotNull(rereadAfString);

        // ensure file length has indeed changed
        assertNotSame(originalLength, testFile.length());
        // check ability to re-read
        AudioFileIO.read(testFile);
    }

    private File createMP4WithExtraByte() throws IOException {
        File orig = new File("testdata", "test.m4a");
        if (!orig.isFile()) {
            System.err.println("Unable to test file - not available");
            return null;
        }
        File testFile = AbstractTestCase.copyAudioToTmp("test.m4a");

        // append a byte
        final FileOutputStream out = new FileOutputStream(testFile, true);
        out.write(-64);
        out.close();

        return testFile;
    }

}
