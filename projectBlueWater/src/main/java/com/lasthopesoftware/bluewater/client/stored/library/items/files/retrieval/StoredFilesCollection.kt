package com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFileEntityInformation
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper
import com.lasthopesoftware.bluewater.repository.fetch
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise

class StoredFilesCollection(private val context: Context) : GetAllStoredFilesInLibrary {
	override fun promiseAllStoredFiles(libraryId: LibraryId): Promise<Collection<StoredFile>> =
		QueuedPromise(MessageWriter {
			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				repositoryAccessHelper
					.mapSql("SELECT * FROM " + StoredFileEntityInformation.tableName + " WHERE " + StoredFileEntityInformation.libraryIdColumnName + " = @" + StoredFileEntityInformation.libraryIdColumnName)
					.addParameter(StoredFileEntityInformation.libraryIdColumnName, libraryId.id)
					.fetch()
			}
		}, ThreadPools.databaseTableExecutor<StoredFile>())
}
