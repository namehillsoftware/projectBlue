package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.storage.GivenAnUnauthenticatedConnection

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FakeFilePropertiesContainer
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FilePropertiesStorage
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.url.MediaServerUrlProvider
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import java.net.URL
import java.util.concurrent.ExecutionException

class WhenStoringFiles {
	companion object Setup {
		private val connectionProvider = mockk<IConnectionProvider>()
		private val fakeFilePropertiesContainer = FakeFilePropertiesContainer()
		private var authenticationRequiredException: SecurityException? = null

		@JvmStatic
		@BeforeClass
		fun context() {
			every { connectionProvider.promiseResponse(*anyVararg()) } returns Promise.empty()

			val urlProvider = MediaServerUrlProvider(null, URL("http://hewo"))
			every { connectionProvider.urlProvider } returns urlProvider

			val fileStorage = FilePropertiesStorage(
				connectionProvider,
				fakeFilePropertiesContainer
			)

			try {
				fileStorage.promiseFileUpdate(ServiceFile(33), "myProperty", "myValue", false).toFuture().get()
			} catch (e: ExecutionException) {
				authenticationRequiredException = e.cause as? SecurityException
			}
		}
	}

	@Test
	fun thenAnAuthenticationExceptionIsThrown() {
		assertThat(authenticationRequiredException).isNotNull
	}

	@Test
	fun thenThePropertyIsNotUpdated() {
		verify(inverse = true) { connectionProvider.promiseResponse(*anyVararg()) }
	}

	@Test
	fun thenTheContainerWasNotUpdated() {
		assertThat(fakeFilePropertiesContainer.getFilePropertiesContainer(UrlKeyHolder("http://hewo", ServiceFile(33)))?.properties).doesNotContainKey("myProperty")
	}
}
