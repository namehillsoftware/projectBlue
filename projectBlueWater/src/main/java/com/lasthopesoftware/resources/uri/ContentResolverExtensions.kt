package com.lasthopesoftware.resources.uri

import android.content.ContentResolver
import android.net.Uri
import java.io.FileNotFoundException

fun ContentResolver.resourceExists(contentUri: Uri) = try {
	openFileDescriptor(contentUri, "r")?.use {
		true
	} ?: false
} catch (_: FileNotFoundException) {
	// False if the file isn't found
	false
}
