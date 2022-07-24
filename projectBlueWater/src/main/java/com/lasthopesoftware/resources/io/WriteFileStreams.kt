package com.lasthopesoftware.resources.io

import java.io.File
import java.io.InputStream

interface WriteFileStreams {
	fun writeStreamToFile(inputStream: InputStream, file: File)
}
