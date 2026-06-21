package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates

import android.database.sqlite.SQLiteConstraintException
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.policies.retries.RecursivePromiseRetryHandler
import com.namehillsoftware.handoff.promises.Promise
import java.util.concurrent.atomic.AtomicInteger

class RetryingStoredFileUpdate(
	private val inner: UpdateStoredFiles
) : UpdateStoredFiles by inner {

	companion object {
		private val logger by lazyLogger<RetryingStoredFileUpdate>()
	}

	override fun promiseStoredFileUpdate(libraryId: LibraryId, serviceFile: ServiceFile): Promise<StoredFile> {
		val attempts = AtomicInteger()
		return RecursivePromiseRetryHandler.retryOnException { error ->
			val attempt = attempts.incrementAndGet()
			if (attempt <= 2 && (error == null || error is SQLiteConstraintException && (error.message?.startsWith("UNIQUE constraint failed")) ?: false)) {
				if (attempt > 1) {
					logger.warn(
						"Failed to update stored file (libraryId=$libraryId, serviceFile=$serviceFile) due to unique constraint failure, retrying.",
						error
					)
				}
				inner.promiseStoredFileUpdate(libraryId, serviceFile)
			} else {
				Promise(error)
			}
		}
	}
}
