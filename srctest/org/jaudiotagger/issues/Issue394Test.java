package org.jaudiotagger.issues;

import org.jaudiotagger.AbstractTestCase;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.id3.AbstractTag;
import org.jaudiotagger.tag.id3.ID3v1Tag;
import org.jaudiotagger.tag.id3.ID3v24Tag;


public class Issue394Test extends AbstractTestCase
{
    public void testCreatingID3v1TagfromID3v2tagWithMultipleComments() throws Exception
    {
        Exception caught = null;
        try
        {
            Tag tag = new ID3v24Tag();
            tag.setField(FieldKey.COMMENT,"COMMENT1");
            tag.addField(FieldKey.COMMENT,"COMMENT2");

            new ID3v1Tag((AbstractTag)tag);
        }
        catch(Exception e)
        {
            caught=e;
            e.printStackTrace();
        }
        assertNull(caught);
    }
}