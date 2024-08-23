/**
 * @author : Paul Taylor
 * @author : Eric Farng
 * <p>
 * Version @version:$Id$
 * <p>
 * MusicTag Copyright (C)2003,2004
 * <p>
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public  License as published by the Free Software Foundation; either version 2.1 of the License,
 * or (at your option) any later version.
 * <p>
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License along with this library; if not,
 * you can get a copy from http://www.opensource.org/licenses/lgpl-license.php or write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 * <p>
 * Description:
 * Base class for all ID3 tags
 */

package com.xuncorp.guqin.media.tag.id3;

/**
 * This is the abstract base class for all ID3 tags.
 *
 * @author : Eric Farng
 * @author : Paul Taylor
 */
public abstract class AbstractID3Tag extends AbstractTag {

    public AbstractID3Tag() {
    }

    protected static final String TAG_RELEASE = "ID3v";
    /**
     * Get full version
     */
    public String getIdentifier() {
        return TAG_RELEASE + getRelease() + "." + getMajorVersion() + "." + getRevision();
    }

    public abstract byte getRelease();

    public abstract byte getMajorVersion();

    public abstract byte getRevision();

    public AbstractID3Tag(AbstractID3Tag copyObject) {
        super(copyObject);
    }

}
