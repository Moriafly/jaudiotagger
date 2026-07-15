/*
 * Jaudiotagger
 * Copyright (C) 2025 Moriafly
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 */

package com.moriafly.jaudiotagger

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

@UnstableJaudiotaggerApi
object JaudiotaggerFlags {
    /**
     * ID3v1 decoding charset.
     */
    @Suppress("MutableBareField")
    @JvmField
    var id3v1DecodingCharset: Charset = StandardCharsets.ISO_8859_1

    /**
     * Charset used when a WAV LIST/INFO chunk is not valid UTF-8.
     *
     * RIFF INFO does not declare its text charset. Leave this as `null` to preserve
     * undecodable bytes losslessly as ISO-8859-1, or set it to the legacy charset
     * used by the source application, such as GB18030.
     *
     * This is a fallback rather than a forced override: valid UTF-8 remains UTF-8.
     */
    @Suppress("MutableBareField")
    @JvmField
    var wavInfoFallbackCharset: Charset? = null
}
