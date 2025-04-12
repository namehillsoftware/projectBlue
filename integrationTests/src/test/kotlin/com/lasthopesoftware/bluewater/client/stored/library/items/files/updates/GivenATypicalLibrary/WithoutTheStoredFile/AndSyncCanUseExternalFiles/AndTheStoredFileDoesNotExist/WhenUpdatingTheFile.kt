package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.GivenATypicalLibrary.WithoutTheStoredFile.AndSyncCanUseExternalFiles.AndTheStoredFileDoesNotExist

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.FakeStoredFileAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.StoredFileUpdater
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.lazyj.Lazy
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.URI
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class WhenUpdatingTheFile {

	companion object {
		private const val libraryId = 814

		private val storedFile = Lazy {
            val storedFileUpdater = StoredFileUpdater(
                FakeStoredFileAccess(),
                mockk {
                    every {
                        promiseUri(
                            LibraryId(libraryId),
                            ServiceFile("4")
                        )
                    } returns Promise.empty()
                },
                mockk {
					every { promiseLibrary(LibraryId(libraryId)) } returns Library(id = libraryId, isUsingExistingFiles = true).toPromise()
				},
                mockk {
                    every {
                        promiseStoredFileUri(
                            LibraryId(libraryId),
                            ServiceFile("4")
                        )
                    } returns Promise(
                        URI("file:/my-public-drive/14/artist/album/my-filename.mp3")
                    )
                },
                mockk(),
            )

            storedFileUpdater
                .promiseStoredFileUpdate(
                    LibraryId(libraryId),
                    ServiceFile("4")
                )
                .toExpiringFuture()
                .get(1, TimeUnit.MINUTES)
        }
	}

	@Test
	fun thenTheFileIsOwnedByTheLibrary() {
		AssertionsForClassTypes.assertThat(storedFile.`object`?.isOwner).isTrue
	}

	@Test
	fun `then the file uri is correct`() {
		AssertionsForClassTypes.assertThat(storedFile.`object`?.uri).isEqualTo("file:/my-public-drive/14/artist/album/my-filename.mp3")
	}
}
