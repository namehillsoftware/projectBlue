package com.lasthopesoftware.bluewater.client.browsing.files.properties.GivenALibraryId.AndStoredFileProperties.AndAWritableConnection

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FakeFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.client.browsing.files.properties.OpenFilePropertiesContainer
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertiesContainer
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertiesUpdatedMessage
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertyStorage
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeLibraryConnectionProvider
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

private const val libraryId = 446
private const val serviceFileId = 111
private const val revision = 416

class WhenUpdatingFileProperties {
	private val deferredRevisionPromise = DeferredPromise(revision)

	private val services by lazy {
        val fakeFileConnectionProvider = FakeConnectionProvider()
        val fakeLibraryConnectionProvider =
            FakeLibraryConnectionProvider(mapOf(Pair(LibraryId(libraryId), fakeFileConnectionProvider)))
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

		val recordingApplicationMessageBus = object : RecordingApplicationMessageBus() {
			override fun <T : ApplicationMessage> sendMessage(message: T) {
				isFilePropertiesContainerUpdatedFirst = isFilePropertiesContainerUpdated
				super.sendMessage(message)
			}
		}

        val filePropertiesStorage = FilePropertyStorage(
			fakeLibraryConnectionProvider,
			mockk {
				every { promiseIsReadOnly(LibraryId(libraryId)) } returns false.toPromise()
			},
			mockk {
				every { promiseRevision(LibraryId(libraryId)) } returns deferredRevisionPromise
			},
			filePropertiesContainer,
			recordingApplicationMessageBus
		)

		Pair(
			Triple(fakeFileConnectionProvider, filePropertiesContainer, recordingApplicationMessageBus),
			filePropertiesStorage)
    }

	private var isFilePropertiesContainerUpdatedFirst = false

	@BeforeAll
	fun act() {
		val (_, storage) = services
		storage
			.promiseFileUpdate(
				LibraryId(libraryId),
				ServiceFile(serviceFileId),
				"package",
				"model",
				false
			)
			.toExpiringFuture()
			.get()
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
				.getFilePropertiesContainer(UrlKeyHolder(URL("http://test:80/MCWS/v1/"), ServiceFile(
                    serviceFileId
                )))
				?.properties!!["package"])
			.isEqualTo("model")
    }

	@Test
	fun `then the properties are updated remotely`() {
		assertThat(
			services
				.first
				.first
				.recordedRequests
				.single())
			.containsExactly(
				"File/SetInfo",
				"File=$serviceFileId",
				"Field=package",
				"Value=model",
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
