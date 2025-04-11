package com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.factory.GivenASubsonicConnection

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.factory.LibraryPlaystatsUpdateSelector
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.settings.SubsonicConnectionSettings
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
		private const val libraryId = 567
	}

	private val scopedPlaystatsUpdateSelector by lazy {
        LibraryPlaystatsUpdateSelector(
            mockk {
                every { promiseConnectionSettings(LibraryId(libraryId)) } returns SubsonicConnectionSettings(
                    url = "",
                    userName = "",
                    password = "",
                ).toPromise()
            },
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
		scopedPlaystatsUpdateSelector.promisePlaystatsUpdate(
            LibraryId(libraryId),
            ServiceFile("ab32872e978b4eb997a6e3f1f24a9f61")
        ).toExpiringFuture().get()
	}

	@Test
	fun `then the file properties playstats update is not called`() {
		assertThat(filePropertiesUpdateCalled).isFalse
	}

	@Test
	fun `then the played signal is sent`() {
		assertThat(playedUpdateCalled).isTrue
	}
}
