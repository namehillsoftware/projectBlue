package com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.GivenATypicalLibrary

import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFileEntityInformation
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.StoredFilesCollection
import com.lasthopesoftware.bluewater.repository.InsertBuilder.Companion.fromTable
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WhenCollectingTheStoredFiles {

	companion object {
		private val storedFiles by lazy {
			RepositoryAccessHelper(ApplicationProvider.getApplicationContext()).use { repositoryAccessHelper ->
				val insertSql = fromTable(StoredFileEntityInformation.tableName)
					.addColumn(StoredFileEntityInformation.serviceIdColumnName)
					.addColumn(StoredFileEntityInformation.libraryIdColumnName)
					.addColumn(StoredFileEntityInformation.isOwnerColumnName)
					.build()
				for (i in 1..9) {
					repositoryAccessHelper
						.mapSql(insertSql)
						.addParameter(StoredFileEntityInformation.serviceIdColumnName, i)
						.addParameter(StoredFileEntityInformation.libraryIdColumnName, 2)
						.addParameter(StoredFileEntityInformation.isOwnerColumnName, true)
						.execute()
				}
				for (i in 13..22) {
					repositoryAccessHelper
						.mapSql(insertSql)
						.addParameter(StoredFileEntityInformation.serviceIdColumnName, i)
						.addParameter(StoredFileEntityInformation.libraryIdColumnName, 5)
						.addParameter(StoredFileEntityInformation.isOwnerColumnName, true)
						.execute()
				}
			}
			val storedFilesCollection =
				StoredFilesCollection(ApplicationProvider.getApplicationContext())

			storedFilesCollection.promiseAllStoredFiles(LibraryId(5)).toFuture().get()
		}
	}

	@Test
	fun thenTheStoredFilesAreFromTheCorrectLibrary() {
		assertThat(storedFiles?.map { it.libraryId }).containsOnly(5)
	}

	@Test
	fun thenTheStoredFilesAreCorrect() {
		assertThat(storedFiles?.map { it.serviceId }).isSubsetOf(13..23)
	}
}
