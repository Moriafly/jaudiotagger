package org.jaudiotagger.tag.id3.framebody;

import org.jaudiotagger.AbstractTestCase;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagField;
import org.jaudiotagger.tag.TagTextField;
import org.jaudiotagger.tag.id3.ID3v23Frames;
import org.jaudiotagger.tag.id3.ID3v24Tag;
import org.jaudiotagger.tag.id3.valuepair.TextEncoding;

import java.io.File;
import java.util.List;

/**
 * Test IPLS
 */
public class FrameBodyIPLSTest extends AbstractTestCase
{
    public static final String INVOLVED_PEOPLE = "producer\0eno,lanois\0engineer\0lillywhite";

    public static FrameBodyIPLS getInitialisedBody()
    {
        FrameBodyIPLS fb = new FrameBodyIPLS();
        fb.setText(FrameBodyIPLSTest.INVOLVED_PEOPLE);
        return fb;
    }

    public void testCreateFrameBody()
    {
        Exception exceptionCaught = null;
        FrameBodyIPLS fb = null;
        try
        {
            fb = new FrameBodyIPLS(TextEncoding.ISO_8859_1, FrameBodyIPLSTest.INVOLVED_PEOPLE);
        }
        catch (Exception e)
        {
            exceptionCaught = e;
        }

        assertNull(exceptionCaught);
        assertEquals(ID3v23Frames.FRAME_ID_V3_INVOLVED_PEOPLE, fb.getIdentifier());
        assertEquals(TextEncoding.ISO_8859_1, fb.getTextEncoding());
        assertEquals("*"+FrameBodyIPLSTest.INVOLVED_PEOPLE+"*", "*"+fb.getText()+"*");
        assertEquals(2,fb.getNumberOfPairs());
        assertEquals("producer",fb.getKeyAtIndex(0));
        assertEquals("eno,lanois",fb.getValueAtIndex(0));
        assertEquals("engineer",fb.getKeyAtIndex(1));
        assertEquals("lillywhite",fb.getValueAtIndex(1));

    }

    public void testCreateFrameBodyEmptyConstructor()
    {
        Exception exceptionCaught = null;
        FrameBodyIPLS fb = null;
        try
        {
            fb = new FrameBodyIPLS();
            fb.setText(FrameBodyIPLSTest.INVOLVED_PEOPLE);
        }
        catch (Exception e)
        {
            exceptionCaught = e;
        }

        assertNull(exceptionCaught);
        assertEquals(ID3v23Frames.FRAME_ID_V3_INVOLVED_PEOPLE, fb.getIdentifier());
        assertEquals(TextEncoding.ISO_8859_1, fb.getTextEncoding());
        assertEquals("*"+FrameBodyIPLSTest.INVOLVED_PEOPLE+"*", "*"+fb.getText()+"*");
        assertEquals(2,fb.getNumberOfPairs());
        assertEquals("producer",fb.getKeyAtIndex(0));
        assertEquals("eno,lanois",fb.getValueAtIndex(0));
        assertEquals("engineer",fb.getKeyAtIndex(1));
        assertEquals("lillywhite",fb.getValueAtIndex(1));

    }

     public void testCreateFromTIPL()
    {
        Exception exceptionCaught = null;
        FrameBodyTIPL fbv4 = FrameBodyTIPLTest.getInitialisedBody();
        FrameBodyIPLS fb = null;
        try
        {
            fb = new FrameBodyIPLS(fbv4);
        }
        catch (Exception e)
        {
            exceptionCaught = e;
        }

        assertNull(exceptionCaught);
        assertEquals(ID3v23Frames.FRAME_ID_V3_INVOLVED_PEOPLE, fb.getIdentifier());
        assertEquals(TextEncoding.ISO_8859_1, fb.getTextEncoding());
        assertEquals("*"+fb.getText()+"*","*"+FrameBodyTIPLTest.INVOLVED_PEOPLE+"*");
        assertEquals(1,fb.getNumberOfPairs());
        assertEquals("producer",fb.getKeyAtIndex(0));
        assertEquals("eno,lanois",fb.getValueAtIndex(0));
    }

    public void testWriteInvolvedPeopleAndDeleteIDv24() throws Exception
    {
        File testFile = AbstractTestCase.copyAudioToTmp("testV1.mp3", new File("testWriteInvolvedPeopleAndDeletev24.mp3"));
        AudioFile f = AudioFileIO.read(testFile);
        assertNull(f.getTag());

        f.setTag(new ID3v24Tag());
        f.getTag().setField(FieldKey.INVOLVEDPEOPLE,"violinist","Nigel Kennedy");
        f.getTag().addField(FieldKey.INVOLVEDPEOPLE,"harpist","Gloria Divosky");
        assertEquals(1,f.getTag().getFieldCount());
        f.commit();
        f = AudioFileIO.read(testFile);
        assertEquals(1,f.getTag().getFields(FieldKey.INVOLVEDPEOPLE).size());
        assertEquals(1,f.getTag().getFieldCount());

        List<String> values = f.getTag().getAll(FieldKey.INVOLVEDPEOPLE);
        assertEquals(2, values.size());
        assertEquals("violinist\0Nigel Kennedy",values.get(0));
        assertEquals("harpist\0Gloria Divosky",values.get(1));
        assertEquals("violinist\0Nigel Kennedy",f.getTag().getFirst(FieldKey.INVOLVEDPEOPLE));
        assertEquals("violinist\0Nigel Kennedy",f.getTag().getValue(FieldKey.INVOLVEDPEOPLE, 0));
        assertEquals("harpist\0Gloria Divosky",f.getTag().getValue(FieldKey.INVOLVEDPEOPLE, 1));

        f.getTag().deleteField(FieldKey.INVOLVEDPEOPLE);
        assertEquals(0,f.getTag().getFieldCount());
        f.commit();
        f = AudioFileIO.read(testFile);
        assertEquals(0,f.getTag().getFields(FieldKey.INVOLVEDPEOPLE).size());
        assertEquals(0,f.getTag().getFieldCount());
        assertEquals(0, f.getTag().getFieldCount());

    }

    public void testWriteInvolvedPeopleAndDeleteIDv24WithProducer() throws Exception
    {
        File testFile = AbstractTestCase.copyAudioToTmp("testV1.mp3", new File("testWriteInvolvedPeopleAndDeletev241.mp3"));
        AudioFile f = AudioFileIO.read(testFile);
        assertNull(f.getTag());

        f.setTag(new ID3v24Tag());
        f.getTag().setField(FieldKey.INVOLVEDPEOPLE,"producer","Nigel Kennedy");
        f.getTag().addField(FieldKey.INVOLVEDPEOPLE,"harpist","Gloria Divosky");
        assertEquals(1,f.getTag().getFieldCount());
        f.commit();
        f = AudioFileIO.read(testFile);
        assertEquals(1,f.getTag().getFields(FieldKey.INVOLVEDPEOPLE).size());
        assertEquals(1,f.getTag().getFieldCount());
        assertEquals(1, f.getTag().getFieldCount());

        List<String> values = f.getTag().getAll(FieldKey.INVOLVEDPEOPLE);
        assertEquals(2, values.size());
        assertEquals("producer\0Nigel Kennedy",values.get(0));
        assertEquals("harpist\0Gloria Divosky",values.get(1));
        assertEquals("producer\0Nigel Kennedy",f.getTag().getFirst(FieldKey.INVOLVEDPEOPLE));
        assertEquals("producer\0Nigel Kennedy",f.getTag().getValue(FieldKey.INVOLVEDPEOPLE, 0));
        assertEquals("harpist\0Gloria Divosky",f.getTag().getValue(FieldKey.INVOLVEDPEOPLE, 1));

        f.getTag().deleteField(FieldKey.INVOLVEDPEOPLE);
        assertEquals(0,f.getTag().getFieldCount());
        f.commit();
        f = AudioFileIO.read(testFile);
        assertEquals(0,f.getTag().getFields(FieldKey.INVOLVEDPEOPLE).size());
        assertEquals(0,f.getTag().getFieldCount());
        assertEquals(0, f.getTag().getFieldCount());
    }

    public void testWriteInvolvedPeopleAndDeleteIDv24WithProducer2() throws Exception
    {
        File testFile = AbstractTestCase.copyAudioToTmp("testV1.mp3", new File("testWriteInvolvedPeopleAndDeletev242.mp3"));
        AudioFile f = AudioFileIO.read(testFile);
        assertNull(f.getTag());

        f.setTag(new ID3v24Tag());
        //This will go into INVOLVEDPEOPLE
        f.getTag().setField(FieldKey.INVOLVEDPEOPLE,"producer\0Nigel Kennedy");

        //SO will this
        f.getTag().addField(FieldKey.INVOLVEDPEOPLE,"harpist","Gloria Divosky");
        assertEquals(1,f.getTag().getFieldCount());
        f.commit();
        f = AudioFileIO.read(testFile);
        assertEquals(1,f.getTag().getFields(FieldKey.INVOLVEDPEOPLE).size());
        assertEquals(1,f.getTag().getFieldCount());
        assertEquals(1, f.getTag().getFieldCount());
        List<TagField> tagFields =  f.getTag().getFields(FieldKey.INVOLVEDPEOPLE);
        for(TagField next:tagFields)
        {
            assertTrue(next instanceof TagTextField);
            assertEquals("producer\0Nigel Kennedy\0harpist\0Gloria Divosky", ((TagTextField)next).getContent());
        }

        //Should see both values as now just two involved people
        List<String> values = f.getTag().getAll(FieldKey.INVOLVEDPEOPLE);
        assertEquals(2, values.size());
        assertEquals("producer\0Nigel Kennedy",values.get(0));
        assertEquals("harpist\0Gloria Divosky",values.get(1));
        assertEquals("producer\0Nigel Kennedy",f.getTag().getFirst(FieldKey.INVOLVEDPEOPLE));
        assertEquals("producer\0Nigel Kennedy",f.getTag().getValue(FieldKey.INVOLVEDPEOPLE, 0));
        assertEquals("harpist\0Gloria Divosky",f.getTag().getValue(FieldKey.INVOLVEDPEOPLE, 1));

        //Should delete all (used to hang onto PRODUCER but no need know has own field really)
        f.getTag().deleteField(FieldKey.INVOLVEDPEOPLE);
        assertEquals(0,f.getTag().getFieldCount());
        f.commit();
        f = AudioFileIO.read(testFile);
        assertEquals(0,f.getTag().getFields(FieldKey.INVOLVEDPEOPLE).size());
        assertEquals(0,f.getTag().getFieldCount());
        assertEquals(0, f.getTag().getFieldCount());

    }
}