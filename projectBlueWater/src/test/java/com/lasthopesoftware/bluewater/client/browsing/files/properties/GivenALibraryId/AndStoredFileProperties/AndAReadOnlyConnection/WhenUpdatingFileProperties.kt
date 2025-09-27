package com.lasthopesoftware.bluewater.client.browsing.files.properties.GivenALibraryId.AndStoredFileProperties.AndAReadOnlyConnection

import com.lasthopesoftware.bluewater.client.access.RemoteLibraryAccess
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertyStorage
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnection
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenUpdatingFileProperties {

	companion object {
		private const val libraryId = 875
		private const val serviceFileId = "946"
	}

	private val services by lazy {
		val recordingApplicationMessageBus = RecordingApplicationMessageBus()

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
			mockk(),
			mockk {
				every { promiseIsReadOnly(LibraryId(libraryId)) } returns true.toPromise()
			},
			recordingApplicationMessageBus,
		)

		Pair(recordingApplicationMessageBus, filePropertiesStorage)
    }

	private val properties = mutableMapOf<String, String>()

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
	}

	@Test
	fun `then no properties are updated`() {
		assertThat(properties).isEmpty()
	}

	@Test
	fun `then a file property update message is NOT sent`() {
		assertThat(
			services
				.first
				.recordedMessages)
			.isEmpty()
	}
}
