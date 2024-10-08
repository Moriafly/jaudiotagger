package com.xuncorp.guqin.media.tag.asf;

import com.xuncorp.guqin.media.audio.asf.data.AsfHeader;
import com.xuncorp.guqin.media.audio.asf.data.MetadataDescriptor;
import com.xuncorp.guqin.media.audio.asf.util.Utils;
import com.xuncorp.guqin.media.tag.TagTextField;

import java.nio.charset.Charset;

/**
 * Represents a tag text field for ASF fields.<br>
 * 
 * @author Christian Laireiter
 */
public class AsfTagTextField extends AsfTagField implements TagTextField {

    /**
     * Creates a tag text field and assigns the string value.
     * 
     * @param field
     *            ASF field to represent.
     * @param value
     *            the value to assign.
     */
    public AsfTagTextField(final AsfFieldKey field, final String value) {
        super(field);
        toWrap.setString(value);
    }

    /**
     * Creates an instance.
     * 
     * @param source
     *            The metadata descriptor, whose content is published.<br>
     *            Must not be of type {@link MetadataDescriptor#TYPE_BINARY}.
     */
    public AsfTagTextField(final MetadataDescriptor source) {
        super(source);
        if (source.getType() == MetadataDescriptor.TYPE_BINARY) {
            throw new IllegalArgumentException(
                    "Cannot interpret binary as string.");
        }
    }

    /**
     * Creates a tag text field and assigns the string value.
     * 
     * @param fieldKey
     *            The fields identifier.
     * @param value
     *            the value to assign.
     */
    public AsfTagTextField(final String fieldKey, final String value) {
        super(fieldKey);
        toWrap.setString(value);
    }

    @Override
    public String getContent() {
        return getDescriptor().getString();
    }

    @Override
    public Charset getEncoding() {
        return AsfHeader.ASF_CHARSET;
    }

    /**
     * @return true if blank or only contains whitespace
     */
    @Override
    public boolean isEmpty() {
        return Utils.isBlank(getContent());
    }

    @Override
    public void setContent(final String content) {
        getDescriptor().setString(content);
    }

    @Override
    public void setEncoding(final Charset encoding) {
        if (!AsfHeader.ASF_CHARSET.equals(encoding)) {
            throw new IllegalArgumentException(
                    "Only UTF-16LE is possible with ASF.");
        }
    }
}
