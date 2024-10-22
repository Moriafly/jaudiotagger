/**
 * @author : Paul Taylor
 * <p>
 * Version @version:$Id$
 * <p>
 * Jaudiotagger Copyright (C)2004,2005
 * <p>
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public  License as published by the Free Software Foundation; either version 2.1 of the License,
 * or (at your option) any later version.
 * <p>
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License ainteger with this library; if not,
 * you can get a copy from http://www.opensource.org/licenses/lgpl-license.php or write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 * <p>
 * Description:
 * Channel type used by
 */
package com.xuncorp.guqin.media.tag.id3.valuepair;

import com.xuncorp.guqin.media.tag.datatype.AbstractIntStringValuePair;

public class ChannelTypes extends AbstractIntStringValuePair {
    private static ChannelTypes channelTypes;

    public static ChannelTypes getInstanceOf() {
        if (channelTypes == null) {
            channelTypes = new ChannelTypes();
        }
        return channelTypes;
    }

    private ChannelTypes() {
        idToValue.put(0x00, "Other");
        idToValue.put(0x01, "Master volume");
        idToValue.put(0x02, "Front right");
        idToValue.put(0x03, "Front left");
        idToValue.put(0x04, "Back right");
        idToValue.put(0x05, "Back left");
        idToValue.put(0x06, "Front centre");
        idToValue.put(0x07, "Back centre");
        idToValue.put(0x08, "Subwoofer");

        createMaps();
    }
}
