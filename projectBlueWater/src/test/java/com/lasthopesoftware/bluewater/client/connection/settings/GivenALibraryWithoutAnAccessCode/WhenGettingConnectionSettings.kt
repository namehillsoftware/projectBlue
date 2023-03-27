package com.lasthopesoftware.bluewater.client.connection.settings.GivenALibraryWithoutAnAccessCode

import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettingsLookup
import com.lasthopesoftware.bluewater.client.connection.settings.MissingAccessCodeException
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.ExecutionException

class WhenGettingConnectionSettings {

	private val mut by lazy {
		val libraryProvider = mockk<ILibraryProvider>()
		every { libraryProvider.promiseLibrary(LibraryId(10)) } returns Library().toPromise()

		val connectionSettingsLookup = ConnectionSettingsLookup(libraryProvider)

		connectionSettingsLookup
	}

	private var exception: MissingAccessCodeException? = null

	@BeforeAll
	fun act() {
		try {
			mut.lookupConnectionSettings(LibraryId(10)).toExpiringFuture().get()
		} catch (e: ExecutionException) {
			exception = e.cause as? MissingAccessCodeException
		}
	}

	@Test
	fun thenAMissingAccessCodeExceptionIsThrown() {
		assertThat(exception).isNotNull
	}
}
