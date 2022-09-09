package com.lasthopesoftware.bluewater.client.browsing.files.properties.GivenStoredFileProperties.AndAWritableConnection

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FakeFilePropertiesContainer
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertiesContainer
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertyStorage
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertyUpdatedMessage
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeLibraryConnectionProvider
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

private const val libraryId = 446
private const val serviceFileId = 111

class WhenUpdatingFileProperties {
	private val services by lazy {
        val fakeFileConnectionProvider = FakeConnectionProvider()
        val fakeLibraryConnectionProvider =
            FakeLibraryConnectionProvider(mapOf(Pair(LibraryId(libraryId), fakeFileConnectionProvider)))
		val filePropertiesContainer = FakeFilePropertiesContainer().apply {
			putFilePropertiesContainer(
				UrlKeyHolder(URL("http://test:80/MCWS/v1/"), ServiceFile(serviceFileId)),
				FilePropertiesContainer(565, mapOf(Pair("package", "heighten")))
			)
		}

		val recordingApplicationMessageBus = RecordingApplicationMessageBus()

        val filePropertiesStorage = FilePropertyStorage(
			fakeLibraryConnectionProvider,
			mockk {
				every { promiseIsReadOnly(LibraryId(libraryId)) } returns false.toPromise()
			},
			mockk {
				every { promiseRevision(LibraryId(libraryId)) } returns 565.toPromise()
			},
			filePropertiesContainer,
			recordingApplicationMessageBus
		)

		Pair(
			Triple(fakeFileConnectionProvider, filePropertiesContainer, recordingApplicationMessageBus),
			filePropertiesStorage)
    }

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
    fun `then the properties are updated in local storage`() {
        assertThat(
			services
				.first
				.second
				.getFilePropertiesContainer(UrlKeyHolder(URL("http://test:80/MCWS/v1/"), ServiceFile(serviceFileId)))
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
				FilePropertyUpdatedMessage(
					LibraryId(libraryId),
					ServiceFile(serviceFileId)
				)
			)
	}
}
