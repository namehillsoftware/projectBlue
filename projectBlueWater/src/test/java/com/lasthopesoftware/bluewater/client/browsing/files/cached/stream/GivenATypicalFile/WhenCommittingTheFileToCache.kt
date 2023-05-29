package com.lasthopesoftware.bluewater.client.browsing.files.cached.stream.GivenATypicalFile

import com.lasthopesoftware.bluewater.client.browsing.files.cached.stream.CachedFileOutputStream
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File

class WhenCommittingTheFileToCache {
	private val mockedFile = mockk<File>()
	private var persistedLibrary: LibraryId? = null
	private var persistedFile: File? = null
	private var persistedKey: String? = null

	@BeforeAll
	fun act() {
		val cachedFileOutputStream = CachedFileOutputStream(
			LibraryId(156),
			"unique-test",
			mockedFile,
			mockk {
				every { putIntoDatabase(any(), any(), any()) } answers {
					persistedLibrary = firstArg()
					persistedKey = secondArg()
					persistedFile = lastArg()
					Promise.empty()
				}
			})
		cachedFileOutputStream.commitToCache().toExpiringFuture().get()
	}

	@Test
	fun `then the correct library is persisted`() {
		assertThat(persistedLibrary).isEqualTo(LibraryId(156))
	}

	@Test
	fun thenTheCorrectKeyIsPersisted() {
		assertThat(persistedKey).isEqualTo("unique-test")
	}

	@Test
	fun thenTheCorrectFileIsPersisted() {
		assertThat(persistedFile).isEqualTo(mockedFile)
	}
}
