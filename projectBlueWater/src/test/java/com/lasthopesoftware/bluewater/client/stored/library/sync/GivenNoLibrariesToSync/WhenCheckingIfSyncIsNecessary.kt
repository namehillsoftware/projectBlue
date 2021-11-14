package com.lasthopesoftware.bluewater.client.stored.library.sync.GivenNoLibrariesToSync

import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.stored.library.items.files.CheckForAnyStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.sync.CollectServiceFilesForSync
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncChecker
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WhenCheckingIfSyncIsNecessary {

	companion object {
		private val isSyncNeeded by lazy {
			val collector = mockk<CollectServiceFilesForSync>()
			every { collector.promiseServiceFilesToSync(any()) } returns Promise(emptySet())

			val checkStoredFiles = mockk<CheckForAnyStoredFiles>()
			with(checkStoredFiles) {
				every { promiseIsAnyStoredFiles(any()) } returns false.toPromise()
			}

			SyncChecker(
				FakeLibraryProvider(
					Library().setId(3),
					Library().setId(11),
					Library().setId(10),
					Library().setId(14)
				),
				collector,
				checkStoredFiles
			)
				.promiseIsSyncNeeded()
				.toFuture()
				.get()
		}
	}

	@Test
	fun thenSyncIsNotNeeded() {
		assertThat(isSyncNeeded).isFalse
	}
}
