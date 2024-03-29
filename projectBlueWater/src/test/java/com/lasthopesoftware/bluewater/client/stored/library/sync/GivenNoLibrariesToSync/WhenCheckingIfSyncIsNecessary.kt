package com.lasthopesoftware.bluewater.client.stored.library.sync.GivenNoLibrariesToSync

import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.stored.library.items.files.CheckForAnyStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.sync.CollectServiceFilesForSync
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncChecker
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenCheckingIfSyncIsNecessary {

	private val isSyncNeeded by lazy {
		val collector = mockk<CollectServiceFilesForSync>()
		every { collector.promiseServiceFilesToSync(any()) } returns Promise(emptySet())

		val checkStoredFiles = mockk<CheckForAnyStoredFiles>()
		with(checkStoredFiles) {
			every { promiseIsAnyStoredFiles(any()) } returns false.toPromise()
		}

		SyncChecker(
			FakeLibraryRepository(
				Library(id = 3),
				Library(id = 11),
				Library(id = 10),
				Library(id = 14)
			),
			collector,
			checkStoredFiles
		)
			.promiseIsSyncNeeded()
			.toExpiringFuture()
			.get()
	}

	@Test
	fun `then sync is not needed`() {
		assertThat(isSyncNeeded).isFalse
	}
}
