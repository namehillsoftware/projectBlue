package com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFileEntityInformation
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper
import com.lasthopesoftware.bluewater.repository.fetchFirst
import com.lasthopesoftware.resources.executors.ThreadPools.promiseTableMessage
import com.namehillsoftware.handoff.promises.Promise

class StoredFileQuery(private val context: Context) : GetStoredFiles {
	override fun promiseStoredFile(libraryId: LibraryId, serviceFile: ServiceFile): Promise<StoredFile> =
		promiseTableMessage<StoredFile, StoredFile> {
			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				repositoryAccessHelper
					.mapSql(
						" SELECT * " +
							" FROM " + StoredFileEntityInformation.tableName + " " +
							" WHERE " + StoredFileEntityInformation.serviceIdColumnName + " = @" + StoredFileEntityInformation.serviceIdColumnName +
							" AND " + StoredFileEntityInformation.libraryIdColumnName + " = @" + StoredFileEntityInformation.libraryIdColumnName
					)
					.addParameter(StoredFileEntityInformation.serviceIdColumnName, serviceFile.key)
					.addParameter(StoredFileEntityInformation.libraryIdColumnName, libraryId.id)
					.fetchFirst()
			}
		}

}
