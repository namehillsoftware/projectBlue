package com.lasthopesoftware.bluewater.client.browsing.files.properties.GivenALibraryId.AndStoredFileProperties.AndAWritableConnection

import com.lasthopesoftware.bluewater.client.access.RemoteLibraryAccess
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
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
		var isFilePropertiesUpdated = false

		val recordingApplicationMessageBus = object : RecordingApplicationMessageBus() {
			override fun <T : ApplicationMessage> sendMessage(message: T) {
				isFilePropertiesUpdatedFirst = isFilePropertiesUpdated
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
								isFilePropertiesUpdated = true
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
			recordingApplicationMessageBus
		)

		Pair(recordingApplicationMessageBus, filePropertiesStorage)
    }

	private var isFilePropertiesUpdatedFirst = false

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
	fun `then the properties are updated before the message is sent`() {
		assertThat(isFilePropertiesUpdatedFirst).isTrue
	}

	@Test
	fun `then the properties are updated remotely`() {
		assertThat(properties.map { e -> Pair(e.key, e.value) }).containsExactly(Pair("package", "model"))
	}

	@Test
	fun `then a file property update message is sent`() {
		assertThat(
			services
				.first
				.recordedMessages
				.single())
			.isEqualTo(
				FilePropertiesUpdatedMessage(
					UrlKeyHolder(URL("http://test:80/MCWS/v1/"), ServiceFile(serviceFileId))
				)
			)
	}
}
