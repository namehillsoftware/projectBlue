package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.storage.GivenAReadOnlyConnection

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FakeFilePropertiesContainer
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.storage.ScopedFilePropertiesStorage
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.CheckScopedRevisions
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfScopedConnectionIsReadOnly
import com.lasthopesoftware.bluewater.client.connection.url.MediaServerUrlProvider
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
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

			val revisionChecker = mockk<CheckScopedRevisions>()
			every { revisionChecker.promiseRevision() } returns 1.toPromise()

			val checkConnection = mockk<CheckIfScopedConnectionIsReadOnly>()
			every { checkConnection.promiseIsReadOnly() } returns true.toPromise()

			val fileStorage = ScopedFilePropertiesStorage(
				connectionProvider,
				checkConnection,
				revisionChecker,
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
		assertThat(fakeFilePropertiesContainer
			.getFilePropertiesContainer(UrlKeyHolder(URL("http://hewo"), ServiceFile(33)))).isNull()
	}
}