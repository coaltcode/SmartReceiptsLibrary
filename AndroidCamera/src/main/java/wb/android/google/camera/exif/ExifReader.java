/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package wb.android.google.camera.exif;

import java.io.IOException;
import java.io.InputStream;

/**
 * This class reads the EXIF header of a JPEG file and stores it in {@link ExifData}.
 */
public class ExifReader {
    /**
     * Parses the inputStream and  and returns the EXIF data in an {@link ExifData}.
     * @throws ExifInvalidFormatException
     * @throws IOException
     */
    public ExifData read(InputStream inputStream) throws ExifInvalidFormatException,
            IOException {
        ExifParser parser = ExifParser.parse(inputStream);
        ExifData exifData = new ExifData(parser.getByteOrder());

        int event = parser.next();
        while (event != ExifParser.EVENT_END) {
            switch (event) {
                case ExifParser.EVENT_START_OF_IFD:
                    exifData.addIfdData(new IfdData(parser.getCurrentIfd()));
                    break;
                case ExifParser.EVENT_NEW_TAG:
                    ExifTag tag = parser.getTag();
                    if (!tag.hasValue()) {
                        parser.registerForTagValue(tag);
                    } else {
                        exifData.getIfdData(tag.getIfd()).setTag(tag);
                    }
                    break;
                case ExifParser.EVENT_VALUE_OF_REGISTERED_TAG:
                    tag = parser.getTag();
                    if (tag.getDataType() == ExifTag.TYPE_UNDEFINED) {
                        byte[] buf = new byte[tag.getComponentCount()];
                        parser.read(buf);
                        tag.setValue(buf);
                    }
                    exifData.getIfdData(tag.getIfd()).setTag(tag);
                    break;
                case ExifParser.EVENT_COMPRESSED_IMAGE:
                    byte buf[] = new byte[parser.getCompressedImageSize()];
                    parser.read(buf);
                    exifData.setCompressedThumbnail(buf);
                    break;
                case ExifParser.EVENT_UNCOMPRESSED_STRIP:
                    buf = new byte[parser.getStripSize()];
                    parser.read(buf);
                    exifData.setStripBytes(parser.getStripIndex(), buf);
                    break;
            }
            event = parser.next();
        }
        return exifData;
    }
}