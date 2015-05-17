package com.lasthopesoftware.bluewater.shared;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by david on 5/17/15.
 */
public class IOCommon {

    /**
     * A little cleaner method than IOUtils, closes the output stream after reading the inputstream
     * into bytes. Credit http://stackoverflow.com/a/17861016
     * @param is
     * @return
     * @throws IOException
     */
    public static byte[] getBytesFromInputStream(final InputStream is) throws IOException
    {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        try
        {
            final byte[] buffer = new byte[0xFFFF];

            for (int len; (len = is.read(buffer)) != -1;)
                os.write(buffer, 0, len);

            os.flush();

            return os.toByteArray();
        } finally {
            os.close();
        }
    }
}
