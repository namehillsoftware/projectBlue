package com.lasthopesoftware.bluewater.client.stored.library.items.files

import android.content.ContentResolver
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileWriteException
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.resources.executors.ThreadPools
import com.lasthopesoftware.resources.io.SupplyFiles
import com.lasthopesoftware.resources.uri.IoCommon
import com.lasthopesoftware.resources.uri.resourceExists
import com.lasthopesoftware.resources.uri.toUri
import com.lasthopesoftware.storage.write.exceptions.StorageCreatePathException
import com.lasthopesoftware.storage.write.permissions.DecideIfFileWriteIsPossible
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.URI

class StoredFileUriDestinationBuilder(
	private val fileSupplier: SupplyFiles,
	private val fileWritePossibleTester: DecideIfFileWriteIsPossible,
	private val contentResolver: ContentResolver
) : ProduceStoredFileDestinations {

	override fun promiseOutputStream(storedFile: StoredFile): Promise<OutputStream?> {
		val storedFileUri = URI(storedFile.uri ?: return Promise.empty())

		return QueuedPromise(MessageWriter {
			when (storedFileUri.scheme) {
				IoCommon.fileUriScheme -> {
					val file = fileSupplier.getFile(storedFileUri)
					val parent = file.parentFile
					if (parent != null && !parent.exists() && !parent.mkdirs()) {
						throw StorageCreatePathException(parent)
					}

					if (!fileWritePossibleTester.isFileWritePossible(file)) {
						throw StoredFileWriteException(storedFile, file)
					}

					file.takeUnless { storedFile.isDownloadComplete && it.exists() }?.let(::FileOutputStream)
				}

				IoCommon.contentUriScheme -> {
					val contentUri = storedFileUri.toUri()

					val isDownloaded = storedFile.isDownloadComplete && contentResolver.resourceExists(contentUri)

					try {
						if (!isDownloaded) contentResolver.openOutputStream(contentUri, "wt") else null
					} catch (f: FileNotFoundException) {
						throw StoredFileWriteException(storedFile, innerException = f)
					}
				}

				else -> null
			}
		}, ThreadPools.io)
	}
}
