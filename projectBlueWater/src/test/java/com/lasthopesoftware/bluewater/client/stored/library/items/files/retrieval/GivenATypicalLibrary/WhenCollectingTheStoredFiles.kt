package com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.GivenATypicalLibrary

import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFileEntityInformation
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.executors.ThreadPools.promiseTableMessage
import com.namehillsoftware.lazyj.Lazy
import com.namehillsoftware.querydroid.SqLiteAssistants.InsertBuilder.fromTable
import org.assertj.core.api.Assertions.assertThat
import org.junit.AfterClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WhenCollectingTheStoredFiles {

	companion object {
		private var storedFiles: Lazy<Collection<StoredFile>?>? = Lazy {
			val insertSql = fromTable(StoredFileEntityInformation.tableName)
				.addColumn(StoredFileEntityInformation.serviceIdColumnName)
				.addColumn(StoredFileEntityInformation.libraryIdColumnName)
				.addColumn(StoredFileEntityInformation.isOwnerColumnName)
				.buildQuery()

			promiseTableMessage<Unit, StoredFile> {
				RepositoryAccessHelper(ApplicationProvider.getApplicationContext()).use { repositoryAccessHelper ->
					repositoryAccessHelper.beginTransaction().use {
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
						it.setTransactionSuccessful()
					}
				}
			}.toExpiringFuture().get()

			val storedFilesCollection = StoredFileAccess(ApplicationProvider.getApplicationContext())
			storedFilesCollection.promiseAllStoredFiles(LibraryId(5)).toExpiringFuture().get()
		}

		@AfterClass
		@JvmStatic
		fun cleanup() {
			storedFiles = null
		}
	}

	@Test
	fun thenTheStoredFilesAreFromTheCorrectLibrary() {
		assertThat(storedFiles?.`object`?.map { it.libraryId }).containsOnly(5)
	}

	@Test
	fun thenTheStoredFilesAreCorrect() {
		assertThat(storedFiles?.`object`?.map { it.serviceId.toInt() }).isSubsetOf(13..23)
	}
}
