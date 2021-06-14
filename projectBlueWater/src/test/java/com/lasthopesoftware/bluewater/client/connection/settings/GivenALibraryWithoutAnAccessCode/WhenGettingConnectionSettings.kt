package com.lasthopesoftware.bluewater.client.connection.settings.GivenALibraryWithoutAnAccessCode

import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettingsLookup
import com.lasthopesoftware.bluewater.client.connection.settings.MissingAccessCodeException
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.ExecutionException

class WhenGettingConnectionSettings {

	companion object setup {

		private var exception: MissingAccessCodeException? = null

		@JvmStatic
		@BeforeClass
		fun context() {
			val libraryProvider = mockk<ILibraryProvider>()
			every { libraryProvider.getLibrary(LibraryId(10)) } returns Library().toPromise()

			val connectionSettingsLookup = ConnectionSettingsLookup(libraryProvider)

			try {
				connectionSettingsLookup.lookupConnectionSettings(LibraryId(10)).toFuture().get()
			} catch (e: ExecutionException) {
				exception = e.cause as? MissingAccessCodeException
			}
		}
	}

	@Test
	fun thenAMissingAccessCodeExceptionIsThrown() {
		assertThat(exception).isNotNull
	}
}
