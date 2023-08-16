package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.GivenATypicalLibrary.WithoutTheStoredFile

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.FakeStoredFileAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.StoredFileUpdater
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.URI
import java.util.concurrent.TimeUnit

class WhenUpdatingTheFile {

	private val affectedSystems by lazy {
		FakeStoredFileAccess()
	}

	private val sut by lazy {
		val fakeLibraryRepository = FakeLibraryRepository(
			Library().setId(14).setSyncedFileLocation(Library.SyncedFileLocation.INTERNAL)
		)

		StoredFileUpdater(
			affectedSystems,
			mockk {
				every { promiseUri(any(), any()) } returns Promise.empty()
			},
			fakeLibraryRepository,
			mockk {
				every { promiseStoredFileUri(LibraryId(14), ServiceFile(4)) } returns Promise(
					URI("file:/my-private-drive/14/artist/album/my-filename.mp3")
				)
			},
			mockk(),
		)
	}

	@BeforeAll
	fun act() {
		sut
			.promiseStoredFileUpdate(LibraryId(14), ServiceFile(4))
			.toExpiringFuture()
			.get(1, TimeUnit.MINUTES)
	}

	@Test
	fun thenTheFileIsOwnedByTheLibrary() {
		assertThat(affectedSystems.storedFiles.values.first { sf -> sf.libraryId == 14 && sf.serviceId == 4 }.isOwner).isTrue
	}

	@Test
	fun `then the file uri is correct`() {
		assertThat(affectedSystems.storedFiles.values.first { sf -> sf.libraryId == 14 && sf.serviceId == 4 }.uri)
			.isEqualTo("file:/my-private-drive/14/artist/album/my-filename.mp3")
	}
}
