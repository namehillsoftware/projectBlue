package com.lasthopesoftware.bluewater.client.browsing.files.access.GivenLibraryFiles.AndTheLibraryRevisionChanges

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.RevisionCachedLibraryFileProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.policies.caching.PermanentCachePolicy
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When the getting the files` {

	companion object {
		private val libraryId = LibraryId(980)
	}

	private val services by lazy {
		RevisionCachedLibraryFileProvider(
			mockk {
				every { promiseFiles(libraryId) } returns listOf(
					ServiceFile("645"),
					ServiceFile("501.5"),
					ServiceFile("CUN7AXL"),
				).toPromise() andThen listOf(
					ServiceFile("R6pgbzfqS9"),
					ServiceFile("7Wtg3R3xi7"),
				).toPromise()
			},
			mockk {
				every { promiseRevision(libraryId) } returns 993L.toPromise() andThen 993L.toPromise() andThen 781L.toPromise()
			},
			PermanentCachePolicy,
		)
	}

	private var initialFiles: List<ServiceFile>? = null
	private var cachedFiles: List<ServiceFile>? = null
	private var filesAfterRevision: List<ServiceFile>? = null

	@BeforeAll
	fun act() {
		initialFiles = services.promiseFiles(libraryId).toExpiringFuture().get()
		cachedFiles = services.promiseFiles(libraryId).toExpiringFuture().get()
		filesAfterRevision = services.promiseFiles(libraryId).toExpiringFuture().get()
	}

	@Test
	fun `then the initial files are correct`() {
		assertThat(initialFiles).isEqualTo(
			listOf(
				ServiceFile("645"),
				ServiceFile("501.5"),
				ServiceFile("CUN7AXL"),
			)
		)
	}

	@Test
	fun `then the cached files are correct`() {
		assertThat(cachedFiles).isEqualTo(
			listOf(
				ServiceFile("645"),
				ServiceFile("501.5"),
				ServiceFile("CUN7AXL"),
			)
		)
	}

	@Test
	fun `then the files change after the revision changes`() {
		assertThat(filesAfterRevision).isEqualTo(
			listOf(
				ServiceFile("R6pgbzfqS9"),
				ServiceFile("7Wtg3R3xi7"),
			)
		)
	}
}
