package com.lasthopesoftware.bluewater.client.stored.library.items.files

import android.content.ContentResolver
import android.content.ContentUris
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileWriteException
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.resources.uri.MediaCollections
import com.lasthopesoftware.storage.write.exceptions.StorageCreatePathException
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.OutputStream

class StoredFileUriDestinationBuilder(
	private val contentResolver: ContentResolver
) : ProduceStoredFileDestinations {
    override fun getFile(storedFile: StoredFile): File? {
        return storedFile.path?.let(::File)
    }

	override fun getOutputStream(storedFile: StoredFile): OutputStream? {
		val storedMediaId = storedFile.storedMediaId.toLong()
		val storedFilePath = storedFile.path

		return when {
			storedFilePath != null -> {
				val file = File(storedFilePath)
				val parent = file.parentFile
				if (parent != null && !parent.exists() && !parent.mkdirs()) {
					throw StorageCreatePathException(parent)
				}

				if (!file.canWrite()) {
					throw StoredFileWriteException(storedFile, file)
				}

				file.takeUnless { it.exists() }?.let(::FileOutputStream)
			}
			storedMediaId > 0 -> {
				val contentUri = ContentUris.withAppendedId(MediaCollections.ExternalAudio, storedMediaId)

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
