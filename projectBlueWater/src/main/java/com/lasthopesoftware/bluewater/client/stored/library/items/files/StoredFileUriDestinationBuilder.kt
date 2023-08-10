package com.lasthopesoftware.bluewater.client.stored.library.items.files

import android.content.ContentResolver
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileWriteException
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.resources.uri.IoCommon
import com.lasthopesoftware.resources.uri.toUri
import com.lasthopesoftware.storage.write.exceptions.StorageCreatePathException
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.URI

class StoredFileUriDestinationBuilder(
	private val contentResolver: ContentResolver
) : ProduceStoredFileDestinations {

	override fun getOutputStream(storedFile: StoredFile): OutputStream? {
		val storedFileUri = URI(storedFile.uri ?: return null)

		return when(storedFileUri.scheme) {
			IoCommon.fileUriScheme -> {
				val file = File(storedFileUri)
				val parent = file.parentFile
				if (parent != null && !parent.exists() && !parent.mkdirs()) {
					throw StorageCreatePathException(parent)
				}

				if (!file.canWrite()) {
					throw StoredFileWriteException(storedFile, file)
				}

				file.takeUnless { it.exists() }?.let(::FileOutputStream)
			}
			IoCommon.contentUriScheme -> {
				val contentUri = storedFileUri.toUri()

				try {
					try {
						contentResolver.openFileDescriptor(contentUri, "r")?.use { return null }
					} catch (f: FileNotFoundException) {
						return contentResolver.openOutputStream(contentUri)
					}

					contentResolver.openOutputStream(contentUri)
				} catch (f: FileNotFoundException) {
					throw StoredFileWriteException(storedFile, innerException = f)
				}
			}
			else -> null
		}
	}
}
