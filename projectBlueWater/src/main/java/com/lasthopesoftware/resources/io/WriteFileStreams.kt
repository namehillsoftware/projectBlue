package com.lasthopesoftware.resources.io

import android.net.Uri
import java.io.File
import java.io.InputStream

interface WriteFileStreams {
	fun writeStreamToFile(inputStream: InputStream, file: File)

	fun writeStreamToUri(inputStream: InputStream, uri: Uri)
}
