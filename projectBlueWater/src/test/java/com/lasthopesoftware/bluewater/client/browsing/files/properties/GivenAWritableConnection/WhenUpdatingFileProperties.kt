package com.lasthopesoftware.bluewater.client.browsing.files.properties.GivenAWritableConnection

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FakeFilePropertiesContainer
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertiesContainer
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertiesUpdatedMessage
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.ScopedFilePropertiesStorage
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.CheckScopedRevisions
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfScopedConnectionIsReadOnly
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.URL

private const val serviceFileId = 782

class WhenUpdatingFileProperties {

	private val services by lazy {
		val fakeConnectionProvider = FakeConnectionProvider()
		val filePropertiesContainer = FakeFilePropertiesContainer().apply {
			putFilePropertiesContainer(
				UrlKeyHolder(URL("http://test:80/MCWS/v1/"), ServiceFile(serviceFileId)),
				FilePropertiesContainer(779, mapOf(Pair("package", "heighten")))
			)
		}

		val revisionChecker = mockk<CheckScopedRevisions>()
		every { revisionChecker.promiseRevision() } returns 779.toPromise()

		val checkConnection = mockk<CheckIfScopedConnectionIsReadOnly>()
		every { checkConnection.promiseIsReadOnly() } returns false.toPromise()

		val recordingApplicationMessageBus = RecordingApplicationMessageBus()

		Pair(
			Triple(
				fakeConnectionProvider,
				filePropertiesContainer,
				recordingApplicationMessageBus,
			),
			ScopedFilePropertiesStorage(
				fakeConnectionProvider,
				checkConnection,
				revisionChecker,
				filePropertiesContainer,
				recordingApplicationMessageBus,
			)
		)
	}

	@BeforeAll
	fun act() {
		val (_, fileStorage) = services
		fileStorage.promiseFileUpdate(ServiceFile(serviceFileId), "slippery", "flood", false).toExpiringFuture().get()
	}

	@Test
	fun `then the properties are updated in local storage`() {
		assertThat(
			services
				.first
				.second
				.getFilePropertiesContainer(UrlKeyHolder(URL("http://test:80/MCWS/v1/"), ServiceFile(serviceFileId)))
				?.properties!!["slippery"])
			.isEqualTo("flood")
	}

	@Test
	fun `then the property is updated`() {
		assertThat(
			services
				.first
				.first
				.recordedRequests
				.single())
			.containsExactly(
				"File/SetInfo",
				"File=$serviceFileId",
				"Field=slippery",
				"Value=flood",
				"formatted=0"
			)
	}

	@Test
	fun `then a file property update message is sent`() {
		assertThat(
			services
				.first
				.third
				.recordedMessages
				.single())
			.isEqualTo(
				FilePropertiesUpdatedMessage(
					UrlKeyHolder(URL("http://test:80/MCWS/v1/"), ServiceFile(serviceFileId))
				)
			)
	}
}
