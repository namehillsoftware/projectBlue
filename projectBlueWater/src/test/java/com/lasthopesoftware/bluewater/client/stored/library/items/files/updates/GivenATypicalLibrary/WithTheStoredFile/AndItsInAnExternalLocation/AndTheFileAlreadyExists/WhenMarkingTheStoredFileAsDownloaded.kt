package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.GivenATypicalLibrary.WithTheStoredFile.AndItsInAnExternalLocation.AndTheFileAlreadyExists

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.FakeStoredFileAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.StoredFileUpdater
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.uri.IoCommon
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.URI

private const val libraryId = 449
private const val serviceFileId = 193
private const val contentUri = "${IoCommon.contentUriScheme}://kitchen"

class WhenMarkingTheStoredFileAsDownloaded {

	private val sut by lazy {
		val fakeLibraryRepository = FakeLibraryRepository(
			Library(
				isUsingExistingFiles = true,
				id = libraryId,
				syncedFileLocation = Library.SyncedFileLocation.INTERNAL
			)
		)

		StoredFileUpdater(
			FakeStoredFileAccess(),
			mockk(),
			fakeLibraryRepository,
			mockk(),
			mockk {
				every { markContentAsNotPending(any()) } answers {
					urisMarkedAsNotPending.add(firstArg())
					Unit.toPromise()
				}
			}
		)
	}

	private val urisMarkedAsNotPending = ArrayList<URI>()
	private var storedFile: StoredFile? = null

	@BeforeAll
	fun act() {
		storedFile = sut
			.markStoredFileAsDownloaded(
				StoredFile(LibraryId(libraryId), ServiceFile(serviceFileId), URI(contentUri), true)
			)
			.toExpiringFuture()
			.get()
	}

	@Test
	fun `then the file is marked as downloaded`() {
		assertThat(storedFile?.isDownloadComplete).isTrue
	}

	@Test
	fun `then the external uri is marked as not pending`() {
		assertThat(urisMarkedAsNotPending).containsExactly(URI("${IoCommon.contentUriScheme}://kitchen"))
	}
}
