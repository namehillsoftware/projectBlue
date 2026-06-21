package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.GivenATypicalLibrary.WithoutTheStoredFile.AndTheUniqueConstraintFailsOnce

import android.database.sqlite.SQLiteConstraintException
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.RetryingStoredFileUpdate
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.Test
import java.util.concurrent.TimeUnit

class WhenUpdatingTheFile : AndroidContext() {

	companion object {
		private const val libraryId = 400
		private const val serviceFileId = "dY10xa2sD"

		private val sut by lazy {
			RetryingStoredFileUpdate(
				mockk {
					every { promiseStoredFileUpdate(LibraryId(libraryId), ServiceFile(serviceFileId)) } returns
						Promise(SQLiteConstraintException("UNIQUE constraint failed: StoredFiles.libraryId, StoredFiles.serviceId (code 2067 SQLITE_CONSTRAINT_UNIQUE)")) andThen
						StoredFile().toPromise()
				},
			)
		}
		private var storedFile: StoredFile? = null
	}

	override fun before() {
		storedFile = sut
			.promiseStoredFileUpdate(LibraryId(libraryId), ServiceFile(serviceFileId))
			.toExpiringFuture()
			.get(1, TimeUnit.MINUTES)
	}

	@Test
	fun `then the file is downloaded`() {
		assertThat(storedFile).isNotNull
	}
}
