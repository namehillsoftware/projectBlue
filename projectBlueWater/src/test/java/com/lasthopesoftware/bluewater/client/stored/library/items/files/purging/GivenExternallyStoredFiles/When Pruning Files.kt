package com.lasthopesoftware.bluewater.client.stored.library.items.files.purging.GivenExternallyStoredFiles

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.FakeStoredFileAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFilesPruner
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.setURI
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.uri.MediaCollections
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URI

private const val libraryId = 718

@RunWith(AndroidJUnit4::class)
class `When Pruning Files` {

	companion object {

		private val storedFileDefinitions by lazy {
			arrayOf(
				Triple(
					LibraryId(libraryId),
					ServiceFile(64),
					URI("${MediaCollections.ExternalAudio}/L86O9Bq")
				),
				Triple(
					LibraryId(libraryId),
					ServiceFile(51),
					URI("${MediaCollections.ExternalAudio}/F91E4"),
				),
				Triple(
					LibraryId(474),
					ServiceFile(890),
					URI("${MediaCollections.ExternalAudio}/1Y2c"),
				),
				Triple(
					LibraryId(libraryId),
					ServiceFile(566),
					URI("${MediaCollections.ExternalAudio}/z1e4CeAA"),
				),
				Triple(
					LibraryId(465),
					ServiceFile(221),
					URI("${MediaCollections.ExternalAudio}/Sk2"),
				),
			)
		}

		private val affectedSystems by lazy {
			FakeStoredFileAccess().apply {
				for ((libraryId, serviceFile, uri) in storedFileDefinitions) {
					promiseNewStoredFile(libraryId, serviceFile).toExpiringFuture().get()!!
						.setIsDownloadComplete(true)
						.setURI(uri)
				}
			}
		}

		private val sut by lazy {
			StoredFilesPruner(
				mockk {
					every { promiseServiceFilesToSync(LibraryId(libraryId)) } returns Promise(
						listOf(
							ServiceFile(890),
							ServiceFile(64),
							ServiceFile(566),
							ServiceFile(516),
						)
					)
				},
				affectedSystems,
				mockk {
					every { removeContent(any()) } answers {
						deletedUris.add(firstArg())
						true.toPromise()
					}
				},
			)
		}

		private val deletedUris = mutableListOf<URI>()

		@BeforeClass
		@JvmStatic
		fun act() {
			sut.pruneStoredFiles(LibraryId(libraryId)).toExpiringFuture().get()
		}
	}

	@Test
	fun `then the correct stored files are deleted from the database`() {
		assertThat(affectedSystems.storedFiles.values.map {
            Pair(
                it.libraryId,
                it.serviceId
            )
        }).containsExactly(
			Pair(libraryId, 64),
			Pair(474, 890),
			Pair(libraryId, 566),
			Pair(465, 221),
		)
	}

	@Test
	fun `then the correct files are deleted`() {
        assertThat(deletedUris)
			.containsExactly(URI("${MediaCollections.ExternalAudio}/F91E4"))
	}
}
