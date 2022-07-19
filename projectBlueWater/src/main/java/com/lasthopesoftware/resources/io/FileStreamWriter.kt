package com.lasthopesoftware.resources.io

import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class FileStreamWriter : WriteFileStreams {

    override fun writeStreamToFile(inputStream: InputStream, file: File) {
        FileOutputStream(file).use { fos ->
            IOUtils.copy(inputStream, fos)
            fos.flush()
        }
    }
}
