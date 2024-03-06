package com.lasthopesoftware.bluewater.client.browsing.files.properties.GivenAWritableConnection

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FakeFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.client.browsing.files.properties.OpenFilePropertiesContainer
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertiesContainer
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertiesUpdatedMessage
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.ScopedFilePropertiesStorage
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.CheckScopedRevisions
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfScopedConnectionIsReadOnly
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.URL

private const val serviceFileId = 782

private const val revision = 779

class WhenUpdatingFileProperties {
	private val deferredRevisionPromise = DeferredPromise(revision)

	private val services by lazy {
		val fakeConnectionProvider = FakeConnectionProvider()

		var isFilePropertiesContainerUpdated = false
		val filePropertiesContainer = FakeFilePropertiesContainerRepository().apply {
			putFilePropertiesContainer(
				UrlKeyHolder(URL("http://test:80/MCWS/v1/"), ServiceFile(serviceFileId)),
				object : OpenFilePropertiesContainer(FilePropertiesContainer(revision, mapOf(Pair("package", "heighten")))) {
					override fun updateProperty(key: String, value: String) {
						isFilePropertiesContainerUpdated = true
						super.updateProperty(key, value)
					}
				}
			)
		}

		val revisionChecker = mockk<CheckScopedRevisions>()
		every { revisionChecker.promiseRevision() } returns deferredRevisionPromise

		val checkConnection = mockk<CheckIfScopedConnectionIsReadOnly>()
		every { checkConnection.promiseIsReadOnly() } returns false.toPromise()

		val recordingApplicationMessageBus = object : RecordingApplicationMessageBus() {
			override fun <T : ApplicationMessage> sendMessage(message: T) {
				isFilePropertiesContainerUpdatedFirst = isFilePropertiesContainerUpdated
				super.sendMessage(message)
			}
		}

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

	private var isFilePropertiesContainerUpdatedFirst = false

	@BeforeAll
	fun act() {
		val (_, fileStorage) = services
		fileStorage.promiseFileUpdate(ServiceFile(serviceFileId), "slippery", "flood", false).toExpiringFuture().get()
		deferredRevisionPromise.resolve()
	}

	@Test
	fun `then the properties are updated in local storage before the message is sent`() {
		assertThat(isFilePropertiesContainerUpdatedFirst).isTrue
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
