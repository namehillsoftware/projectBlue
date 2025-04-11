package com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.factory.GivenTheVersionProviderGivesAnError

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.factory.LibraryPlaystatsUpdateSelector
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.settings.MediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.ExecutionException

class WhenUpdatingPlayStats {

	companion object {
		private const val libraryId = 731
	}

	private val scopedPlaystatsUpdateSelector by lazy {
		LibraryPlaystatsUpdateSelector(
			mockk {
				every { promiseConnectionSettings(LibraryId(libraryId)) } returns MediaCenterConnectionSettings(accessCode = "").toPromise()
			},
			mockk {
				every { promiseServerVersion(LibraryId(libraryId)) } returns Promise(Exception("oops"))
			},
			mockk(),
			mockk(),
		)
	}

	private var exception: Throwable? = null

	@BeforeAll
	fun act() {
		try {
			scopedPlaystatsUpdateSelector.promisePlaystatsUpdate(LibraryId(libraryId), ServiceFile("41"))
				.toExpiringFuture().get()
		} catch (e: ExecutionException) {
			exception = e.cause
		}
	}

	@Test
	fun `then the exception is propagated`() {
		assertThat(exception?.message).isEqualTo("oops")
	}
}
