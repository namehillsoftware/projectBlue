package com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.factory.GivenAVersionOfMediaCenterIsNotReturned

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.factory.LibraryPlaystatsUpdateSelector
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 382

class WhenUpdatingPlayStats {

	private val scopedPlaystatsUpdateSelector by lazy {
		LibraryPlaystatsUpdateSelector(
			mockk {
				every { promiseServerVersion(LibraryId(libraryId)) } returns Promise.empty()
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
		scopedPlaystatsUpdateSelector.promisePlaystatsUpdate(LibraryId(libraryId), ServiceFile(896)).toExpiringFuture().get()
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
