package org.jaudiotagger.tag.id3.framebody;

import org.jaudiotagger.AbstractTestCase;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.id3.*;
import org.jaudiotagger.tag.reference.PerformerHelper;

import java.io.File;


public class InvolvedPeopleTest extends AbstractTestCase
{
    /**
     * Uses TMCL frame
     * @throws Exception
     */
    public void testWriteInvolvedPeopleIDv24() throws Exception
    {
        File testFile = AbstractTestCase.copyAudioToTmp("testV1.mp3", new File("testWritePerformersv24.mp3"));
        AudioFile f = AudioFileIO.read(testFile);
        assertNull(f.getTag());

        f.setTag(new ID3v24Tag());
        f.getTag().setField(FieldKey.INVOLVEDPEOPLE,"violinist","Nigel Kennedy");
        f.getTag().addField(FieldKey.INVOLVEDPEOPLE,"harpist","Gloria Divosky");
        assertEquals(1, f.getTag().getFieldCount());
        assertEquals("violinist\0Nigel Kennedy", f.getTag().getFirst(FieldKey.INVOLVEDPEOPLE));
        assertEquals("violinist\0Nigel Kennedy", f.getTag().getValue(FieldKey.INVOLVEDPEOPLE,0));
        assertEquals("harpist\0Gloria Divosky", f.getTag().getValue(FieldKey.INVOLVEDPEOPLE,1));
        f.commit();
        f = AudioFileIO.read(testFile);
        assertEquals(1,f.getTag().getFields(FieldKey.INVOLVEDPEOPLE).size());
        assertEquals(1,f.getTag().getFieldCount());
        assertEquals(1, f.getTag().getFieldCount());
    }

    public void testWriteInvolvedPeopleAndDeleteIDv24() throws Exception
    {
        File testFile = AbstractTestCase.copyAudioToTmp("testV1.mp3", new File("testWritePerformersAndDeletev24.mp3"));
        AudioFile f = AudioFileIO.read(testFile);
        assertNull(f.getTag());

        f.setTag(new ID3v24Tag());
        f.getTag().setField(FieldKey.PERFORMER,"violinist","Nigel Kennedy");
        f.getTag().addField(FieldKey.PERFORMER,"harpist","Gloria Divosky");
        assertEquals(1,f.getTag().getFieldCount());
        f.commit();
        f = AudioFileIO.read(testFile);
        assertEquals(1,f.getTag().getFields(FieldKey.PERFORMER).size());
        assertEquals(1,f.getTag().getFieldCount());
        assertEquals(1, f.getTag().getFieldCount());

        f.getTag().deleteField(FieldKey.PERFORMER);
        assertEquals(0,f.getTag().getFieldCount());
        f.commit();
        f = AudioFileIO.read(testFile);
        assertEquals(0,f.getTag().getFields(FieldKey.PERFORMER).size());
        assertEquals(0,f.getTag().getFieldCount());
        assertEquals(0, f.getTag().getFieldCount());

    }

    public void testWriteInvolvedPeopleIDv23() throws Exception
    {
        File testFile = AbstractTestCase.copyAudioToTmp("testV1.mp3", new File("testWritePerformersv23.mp3"));
        AudioFile f = AudioFileIO.read(testFile);
        assertNull(f.getTag());

        f.setTag(new ID3v23Tag());
        f.getTag().setField(FieldKey.INVOLVEDPEOPLE,"violinist","Nigel Kennedy");
        f.getTag().addField(FieldKey.INVOLVEDPEOPLE,"harpist","Gloria Divosky");
        assertEquals(1,f.getTag().getFieldCount());
        assertEquals("violinist\0Nigel Kennedy", f.getTag().getFirst(FieldKey.INVOLVEDPEOPLE));
        assertEquals("violinist\0Nigel Kennedy", f.getTag().getValue(FieldKey.INVOLVEDPEOPLE,0));
        assertEquals("harpist\0Gloria Divosky", f.getTag().getValue(FieldKey.INVOLVEDPEOPLE,1));
        f.commit();
        f = AudioFileIO.read(testFile);
        assertEquals(1,f.getTag().getFields(FieldKey.INVOLVEDPEOPLE).size());
        assertEquals(1,f.getTag().getFieldCount());
        assertEquals(1, f.getTag().getFieldCount());
    }

    public void testWriteInvolvedPeopleIDv22() throws Exception
    {
        File testFile = AbstractTestCase.copyAudioToTmp("testV1.mp3", new File("testWritePerformersv22.mp3"));
        AudioFile f = AudioFileIO.read(testFile);
        assertNull(f.getTag());

        f.setTag(new ID3v22Tag());
        f.getTag().setField(FieldKey.INVOLVEDPEOPLE,"violinist","Nigel Kennedy");
        f.getTag().addField(FieldKey.INVOLVEDPEOPLE,"harpist","Gloria Divosky");
        assertEquals(1,f.getTag().getFieldCount());
        assertEquals("violinist\0Nigel Kennedy", f.getTag().getFirst(FieldKey.INVOLVEDPEOPLE));
        assertEquals("violinist\0Nigel Kennedy", f.getTag().getValue(FieldKey.INVOLVEDPEOPLE,0));
        assertEquals("harpist\0Gloria Divosky", f.getTag().getValue(FieldKey.INVOLVEDPEOPLE,1));
        f.commit();
        f = AudioFileIO.read(testFile);
        assertEquals(1,f.getTag().getFields(FieldKey.INVOLVEDPEOPLE).size());
        assertEquals(1,f.getTag().getFieldCount());
        assertEquals(1, f.getTag().getFieldCount());
    }

    /**
     * Uses TMCL frame
     * @throws Exception
     */
    public void testWriteInvolvedPeopleIDv24v2() throws Exception
    {
        File testFile = AbstractTestCase.copyAudioToTmp("testV1.mp3", new File("testWritePerformersv24.mp3"));
        AudioFile f = AudioFileIO.read(testFile);
        assertNull(f.getTag());

        f.setTag(new ID3v24Tag());
        f.getTag().setField(FieldKey.INVOLVEDPEOPLE, "violinist\0Nigel Kennedy");
        f.getTag().addField(FieldKey.INVOLVEDPEOPLE, "harpist\0Gloria Divosky");
        assertEquals(1, f.getTag().getFieldCount());
        assertEquals("violinist\0Nigel Kennedy", f.getTag().getValue(FieldKey.INVOLVEDPEOPLE,0));
        assertEquals("harpist\0Gloria Divosky", f.getTag().getValue(FieldKey.INVOLVEDPEOPLE,1));
        assertEquals("violinist\0Nigel Kennedy", f.getTag().getFirst(FieldKey.INVOLVEDPEOPLE));
        f.commit();
        f = AudioFileIO.read(testFile);
        assertEquals(1,f.getTag().getFields(FieldKey.INVOLVEDPEOPLE).size());
        assertEquals(1,f.getTag().getFieldCount());
        assertEquals(1, f.getTag().getFieldCount());
    }

    /**
     * Uses TMCL frame
     * @throws Exception
     */
    public void testWriteInvolvedPeopleIDv24v3() throws Exception
    {
        File testFile = AbstractTestCase.copyAudioToTmp("testV1.mp3", new File("testWritePerformersv24.mp3"));
        AudioFile f = AudioFileIO.read(testFile);
        assertNull(f.getTag());

        f.setTag(new ID3v24Tag());
        f.getTag().setField(FieldKey.INVOLVEDPEOPLE, PerformerHelper.formatForId3("Nigel Kennedy","violinist"));
        f.getTag().setField(FieldKey.INVOLVEDPEOPLE, PerformerHelper.formatForId3("Gloria Divosky","harpist"));
        assertEquals(1, f.getTag().getFieldCount());
        assertEquals("violinist\0Nigel Kennedy", f.getTag().getFirst(FieldKey.INVOLVEDPEOPLE));
        assertEquals("violinist\0Nigel Kennedy", f.getTag().getValue(FieldKey.INVOLVEDPEOPLE,0));
        assertEquals("harpist\0Gloria Divosky", f.getTag().getValue(FieldKey.INVOLVEDPEOPLE,1));
        f.commit();
        f = AudioFileIO.read(testFile);
        assertEquals(1,f.getTag().getFields(FieldKey.INVOLVEDPEOPLE).size());
        assertEquals(1,f.getTag().getFieldCount());
        assertEquals(1, f.getTag().getFieldCount());
    }

    public void testWriteInvolvedPeopleIDv23v2() throws Exception
    {
        File testFile = AbstractTestCase.copyAudioToTmp("testV1.mp3", new File("testWritePerformersv23.mp3"));
        AudioFile f = AudioFileIO.read(testFile);
        assertNull(f.getTag());

        f.setTag(new ID3v23Tag());
        f.getTag().setField(FieldKey.INVOLVEDPEOPLE, "violinist\0Nigel Kennedy");
        f.getTag().addField(FieldKey.INVOLVEDPEOPLE, "harpist\0Gloria Divosky");
        assertEquals(1,f.getTag().getFieldCount());
        assertEquals("violinist\0Nigel Kennedy", f.getTag().getFirst(FieldKey.INVOLVEDPEOPLE));
        assertEquals("violinist\0Nigel Kennedy", f.getTag().getValue(FieldKey.INVOLVEDPEOPLE,0));
        assertEquals("harpist\0Gloria Divosky", f.getTag().getValue(FieldKey.INVOLVEDPEOPLE,1));
        f.commit();
        f = AudioFileIO.read(testFile);
        assertEquals(1,f.getTag().getFields(FieldKey.INVOLVEDPEOPLE).size());
        assertEquals(1,f.getTag().getFieldCount());
        assertEquals(1, f.getTag().getFieldCount());
    }

    public void testWriteInvolvedPeopleIDv23v3() throws Exception
    {
        File testFile = AbstractTestCase.copyAudioToTmp("testV1.mp3", new File("testWritePerformersv23.mp3"));
        AudioFile f = AudioFileIO.read(testFile);
        assertNull(f.getTag());

        f.setTag(new ID3v23Tag());
        f.getTag().setField(FieldKey.INVOLVEDPEOPLE, PerformerHelper.formatForId3("Nigel Kennedy","violinist"));
        f.getTag().setField(FieldKey.INVOLVEDPEOPLE, PerformerHelper.formatForId3("Gloria Divosky","harpist"));
        assertEquals(1,f.getTag().getFieldCount());
        assertEquals("violinist\0Nigel Kennedy", f.getTag().getFirst(FieldKey.INVOLVEDPEOPLE));
        assertEquals("violinist\0Nigel Kennedy", f.getTag().getValue(FieldKey.INVOLVEDPEOPLE,0));
        assertEquals("harpist\0Gloria Divosky", f.getTag().getValue(FieldKey.INVOLVEDPEOPLE,1));
        f.commit();
        f = AudioFileIO.read(testFile);
        assertEquals(1,f.getTag().getFields(FieldKey.INVOLVEDPEOPLE).size());
        assertEquals(1,f.getTag().getFieldCount());
        assertEquals(1, f.getTag().getFieldCount());
    }

    public void testWriteInvolvedPeopleIDv22v2() throws Exception
    {
        File testFile = AbstractTestCase.copyAudioToTmp("testV1.mp3", new File("testWriteInvolvedPeoplev22.mp3"));
        AudioFile f = AudioFileIO.read(testFile);
        assertNull(f.getTag());

        f.setTag(new ID3v22Tag());
        f.getTag().setField(FieldKey.INVOLVEDPEOPLE, "violinist\0Nigel Kennedy");
        f.getTag().addField(FieldKey.INVOLVEDPEOPLE, "harpist\0Gloria Divosky");
        assertEquals(1,f.getTag().getFieldCount());
        assertEquals("violinist\0Nigel Kennedy", f.getTag().getFirst(FieldKey.INVOLVEDPEOPLE));
        assertEquals("violinist\0Nigel Kennedy", f.getTag().getValue(FieldKey.INVOLVEDPEOPLE,0));
        assertEquals("harpist\0Gloria Divosky", f.getTag().getValue(FieldKey.INVOLVEDPEOPLE,1));
        f.commit();
        f = AudioFileIO.read(testFile);
        assertEquals(1,f.getTag().getFields(FieldKey.INVOLVEDPEOPLE).size());
        assertEquals(1,f.getTag().getFieldCount());
        assertEquals(1, f.getTag().getFieldCount());
    }

    public void testWriteInvolvedPeopleIDv22v3() throws Exception
    {
        File testFile = AbstractTestCase.copyAudioToTmp("testV1.mp3", new File("testWritePerformersv22.mp3"));
        AudioFile f = AudioFileIO.read(testFile);
        assertNull(f.getTag());

        f.setTag(new ID3v22Tag());
        f.getTag().setField(FieldKey.INVOLVEDPEOPLE, PerformerHelper.formatForId3("Nigel Kennedy","violinist"));
        f.getTag().setField(FieldKey.INVOLVEDPEOPLE, PerformerHelper.formatForId3("Gloria Divosky","harpist"));
        assertEquals(1,f.getTag().getFieldCount());
        assertEquals("violinist\0Nigel Kennedy", f.getTag().getFirst(FieldKey.INVOLVEDPEOPLE));
        assertEquals("violinist\0Nigel Kennedy", f.getTag().getValue(FieldKey.INVOLVEDPEOPLE,0));
        assertEquals("harpist\0Gloria Divosky", f.getTag().getValue(FieldKey.INVOLVEDPEOPLE,1));
        f.commit();
        f = AudioFileIO.read(testFile);
        assertEquals(1,f.getTag().getFields(FieldKey.INVOLVEDPEOPLE).size());
        assertEquals(1,f.getTag().getFieldCount());
        assertEquals(1, f.getTag().getFieldCount());

    }

    public void testWriteMultiplePeopleIDv24() throws Exception
    {
        File testFile = AbstractTestCase.copyAudioToTmp("testV1.mp3", new File("testWriteMultiplePeoplev24.mp3"));
        AudioFile f = AudioFileIO.read(testFile);
        assertNull(f.getTag());

        f.setTag(new ID3v24Tag());
        f.getTag().addField(FieldKey.INVOLVEDPEOPLE,"producer","steve lilllywhite");
        f.getTag().addField(FieldKey.INVOLVEDPEOPLE,"harpist","Gloria Divosky");
        assertEquals(1,f.getTag().getFieldCount());
        assertEquals("producer\0steve lilllywhite", f.getTag().getValue(FieldKey.INVOLVEDPEOPLE,0));
        assertEquals("harpist\0Gloria Divosky", f.getTag().getValue(FieldKey.INVOLVEDPEOPLE,1));

        f.commit();
        f = AudioFileIO.read(testFile);
        assertEquals(1,f.getTag().getFields(FieldKey.INVOLVEDPEOPLE).size());
        assertEquals(1,f.getTag().getFieldCount());

        f.getTag().deleteField(FieldKey.INVOLVEDPEOPLE);
        assertEquals(0,f.getTag().getFields(FieldKey.INVOLVEDPEOPLE).size());
        assertEquals(0,f.getTag().getFieldCount());
        assertEquals(0, f.getTag().getFieldCount());

        f.commit();
        f = AudioFileIO.read(testFile);
        assertEquals(0,f.getTag().getFields(FieldKey.INVOLVEDPEOPLE).size());
        assertEquals(0,f.getTag().getFieldCount());
        assertEquals(0, f.getTag().getFieldCount());
    }

    public void testWriteMultiplePeopleIDv23() throws Exception
    {
        File testFile = AbstractTestCase.copyAudioToTmp("testV1.mp3", new File("testWriteMultiplePeoplev23.mp3"));
        AudioFile f = AudioFileIO.read(testFile);
        assertNull(f.getTag());

        f.setTag(new ID3v23Tag());
        f.getTag().addField(FieldKey.INVOLVEDPEOPLE,"producer","steve lilllywhite");
        f.getTag().addField(FieldKey.INVOLVEDPEOPLE,"harpist","Gloria Divosky");
        assertEquals(1,f.getTag().getFieldCount());
        assertEquals("producer\0steve lilllywhite", f.getTag().getValue(FieldKey.INVOLVEDPEOPLE,0));
        assertEquals("harpist\0Gloria Divosky", f.getTag().getValue(FieldKey.INVOLVEDPEOPLE,1));

        f.commit();
        f = AudioFileIO.read(testFile);
        assertEquals(1,f.getTag().getFields(FieldKey.INVOLVEDPEOPLE).size());
        assertEquals(1,f.getTag().getFieldCount());

        f.getTag().deleteField(FieldKey.INVOLVEDPEOPLE);
        assertEquals(0,f.getTag().getFields(FieldKey.INVOLVEDPEOPLE).size());
        assertEquals(0,f.getTag().getFieldCount());
        assertEquals(0, f.getTag().getFieldCount());

        f.commit();
        f = AudioFileIO.read(testFile);
        assertEquals(0,f.getTag().getFields(FieldKey.INVOLVEDPEOPLE).size());
        assertEquals(0,f.getTag().getFieldCount());
        assertEquals(0, f.getTag().getFieldCount());
    }

    public void testWriteMultiplePeopleIDv22() throws Exception
    {
        File testFile = AbstractTestCase.copyAudioToTmp("testV1.mp3", new File("testWriteMultiplePeoplev22.mp3"));
        AudioFile f = AudioFileIO.read(testFile);
        assertNull(f.getTag());

        f.setTag(new ID3v22Tag());
        f.getTag().addField(FieldKey.INVOLVEDPEOPLE,"producer","steve lilllywhite");
        f.getTag().addField(FieldKey.INVOLVEDPEOPLE,"harpist","Gloria Divosky");
        assertEquals(1,f.getTag().getFieldCount());
        assertEquals("producer\0steve lilllywhite", f.getTag().getValue(FieldKey.INVOLVEDPEOPLE,0));
        assertEquals("harpist\0Gloria Divosky", f.getTag().getValue(FieldKey.INVOLVEDPEOPLE,1));

        f.commit();
        f = AudioFileIO.read(testFile);
        assertEquals(1,f.getTag().getFields(FieldKey.INVOLVEDPEOPLE).size());
        assertEquals(1,f.getTag().getFieldCount());

        f.getTag().deleteField(FieldKey.INVOLVEDPEOPLE);
        assertEquals(0,f.getTag().getFields(FieldKey.INVOLVEDPEOPLE).size());
        assertEquals(0,f.getTag().getFieldCount());
        assertEquals(0, f.getTag().getFieldCount());

        f.commit();
        f = AudioFileIO.read(testFile);
        assertEquals(0,f.getTag().getFields(FieldKey.INVOLVEDPEOPLE).size());
        assertEquals(0,f.getTag().getFieldCount());
        assertEquals(0, f.getTag().getFieldCount());
    }
}