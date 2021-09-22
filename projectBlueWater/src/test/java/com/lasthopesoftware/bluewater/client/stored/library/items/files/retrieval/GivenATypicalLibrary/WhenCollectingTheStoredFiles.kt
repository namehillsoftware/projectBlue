package com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.GivenATypicalLibrary

import androidx.test.core.app.ApplicationProvider
import com.annimon.stream.Stream
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFileEntityInformation
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.StoredFilesCollection
import com.lasthopesoftware.bluewater.repository.InsertBuilder.Companion.fromTable
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.robolectric.RuntimeEnvironment

class WhenCollectingTheStoredFiles : AndroidContext() {
	@Throws(Exception::class)
	override fun before() {
		RepositoryAccessHelper(RuntimeEnvironment.application).use { repositoryAccessHelper ->
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
		storedFiles = FuturePromise(storedFilesCollection.promiseAllStoredFiles(LibraryId(5))).get()
	}

	@Test
	fun thenTheStoredFilesAreFromTheCorrectLibrary() {
		assertThat(storedFiles?.map { obj -> obj.libraryId }).containsOnly(5)
	}

	@Test
	fun thenTheStoredFilesAreCorrect() {
		assertThat(storedFiles?.map { obj -> obj.serviceId }).isSubsetOf(Stream.range(13, 23).toList())
	}

	companion object {
		private var storedFiles: Collection<StoredFile>? = null
	}
}
