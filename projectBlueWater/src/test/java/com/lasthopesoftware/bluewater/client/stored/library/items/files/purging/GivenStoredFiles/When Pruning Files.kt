package com.lasthopesoftware.bluewater.client.stored.library.items.files.purging.GivenStoredFiles

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.FakeStoredFileAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFilesPruner
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.setURI
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File

class `When Pruning Files` {

	companion object {
		private const val libraryId = 86
	}

	private val storedFileDefinitions by lazy {
		mapOf(
			Pair(
				Pair(
					LibraryId(libraryId),
					ServiceFile("572")
				),
				File.createTempFile("8OI5hu", "q52Pp5JR").apply { deleteOnExit() }
			),
			Pair(
				Pair(
					LibraryId(libraryId),
					ServiceFile("221")
				),
				File.createTempFile("8OI5hu", "q52Pp5JR").apply { deleteOnExit() }
			),
			Pair(
				Pair(
					LibraryId(libraryId),
					ServiceFile("949")
				),
				File.createTempFile("8OI5hu", "q52Pp5JR").apply { deleteOnExit() }
			),
			Pair(
				Pair(
					LibraryId(465),
					ServiceFile("221")
				),
				File.createTempFile("8OI5hu", "q52Pp5JR").apply { deleteOnExit() }
			),
		)
	}

	private val affectedSystems by lazy {
		FakeStoredFileAccess().apply {
			for ((key, file) in storedFileDefinitions) {
				val (libraryId, serviceFile) = key
				promiseNewStoredFile(libraryId, serviceFile).toExpiringFuture().get()!!
					.setIsDownloadComplete(true)
					.setURI(file.toURI())
			}
		}
	}

	private val sut by lazy {
		StoredFilesPruner(
			mockk {
				every { promiseServiceFilesToSync(LibraryId(libraryId)) } returns Promise(
					listOf(
						ServiceFile("572"),
						ServiceFile("949"),
					)
				)
			},
			affectedSystems,
			mockk(),
		)
	}

	@BeforeAll
	fun act() {
		sut.pruneStoredFiles(LibraryId(libraryId)).toExpiringFuture().get()
	}

	@Test
	fun `then the correct stored files are deleted from the database`() {
		assertThat(affectedSystems.storedFiles.values.map { Pair(it.libraryId, it.serviceId) }).containsExactly(
			Pair(libraryId, "572"),
			Pair(libraryId, "949"),
			Pair(465, "221"),
		)
	}

	@Test
	fun `then the correct files exist`() {
		assertThat(storedFileDefinitions.map { Triple(it.key.first, it.key.second, it.value.exists()) })
			.containsExactly(
				Triple(LibraryId(libraryId), ServiceFile("572"), true),
				Triple(LibraryId(libraryId), ServiceFile("221"), false),
				Triple(LibraryId(libraryId), ServiceFile("949"), true),
				Triple(LibraryId(465), ServiceFile("221"), true),
			)
	}
}
