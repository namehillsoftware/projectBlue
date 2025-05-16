package com.lasthopesoftware.bluewater.client.browsing.files.properties.GivenALibraryId.AndStoredFileProperties.AndAWritableConnection

import com.lasthopesoftware.bluewater.client.access.RemoteLibraryAccess
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FakeFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.client.browsing.files.properties.OpenFilePropertiesContainer
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertiesContainer
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertiesUpdatedMessage
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertyStorage
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnection
import com.lasthopesoftware.bluewater.client.connection.url.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.URL

class WhenUpdatingFileProperties {

	companion object {
		private const val libraryId = 446
		private const val serviceFileId = "111"
		private const val revision = 416L
	}

	private val deferredRevisionPromise = DeferredPromise(revision)
	private val properties = mutableMapOf<String, String>()

	private val services by lazy {
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
			mockk {
				every { promiseLibraryConnection(LibraryId(libraryId)) } returns Promise(
					mockk<LiveServerConnection> {
						every { dataAccess } returns mockk<RemoteLibraryAccess> {
							every {
								promiseFilePropertyUpdate(
									ServiceFile(serviceFileId),
									any(),
									any(),
									false
								)
							} answers {
								properties[secondArg()] = thirdArg()
								Unit.toPromise()
							}
						}
					}
				)
			},
			mockk {
				every { promiseUrlKey(LibraryId(libraryId), ServiceFile(serviceFileId)) } returns Promise(
					UrlKeyHolder(URL("http://test:80/MCWS/v1/"), ServiceFile(serviceFileId))
				)
			},
			mockk {
				every { promiseIsReadOnly(LibraryId(libraryId)) } returns false.toPromise()
			},
			mockk {
				every { promiseRevision(LibraryId(libraryId)) } returns deferredRevisionPromise
			},
			filePropertiesContainer,
			recordingApplicationMessageBus
		)

		Triple(filePropertiesContainer, recordingApplicationMessageBus, filePropertiesStorage)
    }

	private var isFilePropertiesContainerUpdatedFirst = false

	@BeforeAll
	fun act() {
		val (_, _, storage) = services
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
				.getFilePropertiesContainer(
					UrlKeyHolder(URL("http://test:80/MCWS/v1/"), ServiceFile(
                    serviceFileId
                ))
				)
				?.properties!!["package"])
			.isEqualTo("model")
    }

	@Test
	fun `then the properties are updated remotely`() {
		assertThat(properties.map { e -> Pair(e.key, e.value) }).containsExactly(Pair("package", "model"))
	}

	@Test
	fun `then a file property update message is sent`() {
		assertThat(
			services
				.second
				.recordedMessages
				.single())
			.isEqualTo(
				FilePropertiesUpdatedMessage(
					UrlKeyHolder(URL("http://test:80/MCWS/v1/"), ServiceFile(serviceFileId))
				)
			)
	}
}
