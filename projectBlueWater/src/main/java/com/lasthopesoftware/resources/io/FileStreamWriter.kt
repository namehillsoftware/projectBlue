package com.lasthopesoftware.resources.io

import android.content.ContentResolver
import android.net.Uri
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class FileStreamWriter(private val contentResolver: ContentResolver) : WriteFileStreams {

    override fun writeStreamToFile(inputStream: InputStream, file: File) {
        FileOutputStream(file).use { fos ->
            IOUtils.copy(inputStream, fos)
            fos.flush()
        }
    }

	override fun writeStreamToUri(inputStream: InputStream, uri: Uri) {
		contentResolver.openOutputStream(uri, "w")?.use { os ->
			IOUtils.copy(inputStream, os)
			os.flush()
		}
	}
}
