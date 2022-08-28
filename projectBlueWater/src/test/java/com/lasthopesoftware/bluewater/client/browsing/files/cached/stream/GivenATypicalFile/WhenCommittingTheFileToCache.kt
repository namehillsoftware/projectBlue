package com.lasthopesoftware.bluewater.client.browsing.files.cached.stream.GivenATypicalFile

import com.lasthopesoftware.bluewater.client.browsing.files.cached.persistence.IDiskFileCachePersistence
import com.lasthopesoftware.bluewater.client.browsing.files.cached.stream.CachedFileOutputStream
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
	private var persistedFile: File? = null
	private var persistedKey: String? = null

	@BeforeAll
	fun act() {
		val cachedFileOutputStream = CachedFileOutputStream(
			"unique-test",
			mockedFile,
			mockk<IDiskFileCachePersistence>().apply {
				every { putIntoDatabase(any(), any()) } answers {
					persistedKey = firstArg()
					persistedFile = lastArg()
					Promise.empty()
				}
			})
		cachedFileOutputStream.commitToCache().toExpiringFuture().get()
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
