package com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.factory.GivenVersion21OrLessOfMediaCenter

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.factory.LibraryPlaystatsUpdateSelector
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.settings.MediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.client.servers.version.SemanticVersion
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenUpdatingPlayStats {

	companion object {
		private const val libraryId = 342
	}

	private val scopedPlaystatsUpdateSelector by lazy {
		LibraryPlaystatsUpdateSelector(
			mockk {
				every { promiseConnectionSettings(LibraryId(libraryId)) } returns MediaCenterConnectionSettings(accessCode = "").toPromise()
			},
			mockk {
				every { promiseServerVersion(LibraryId(libraryId)) } returns Promise(SemanticVersion(21, 0, 0))
			},
			mockk {
				every { promisePlaystatsUpdate(LibraryId(libraryId), any()) } answers {
					playedUpdateCalled = true
					Unit.toPromise()
				}
			},
			mockk {
				every { promisePlaystatsUpdate(LibraryId(libraryId), any()) } answers {
					filePropertiesUpdateCalled = true
					Unit.toPromise()
				}
			},
		)
	}

	private var playedUpdateCalled = false
	private var filePropertiesUpdateCalled = false

	@BeforeAll
	fun act() {
		scopedPlaystatsUpdateSelector.promisePlaystatsUpdate(LibraryId(libraryId), ServiceFile("748")).toExpiringFuture().get()
	}

	@Test
	fun `then the file properties playstats updater is given`() {
		assertThat(filePropertiesUpdateCalled).isTrue
	}

	@Test
	fun `then the played signal is not sent`() {
		assertThat(playedUpdateCalled).isFalse
	}
}
