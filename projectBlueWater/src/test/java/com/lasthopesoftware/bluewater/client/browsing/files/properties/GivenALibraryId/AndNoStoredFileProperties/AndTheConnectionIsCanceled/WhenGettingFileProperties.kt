package com.lasthopesoftware.bluewater.client.browsing.files.properties.GivenALibraryId.AndNoStoredFileProperties.AndTheConnectionIsCanceled

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeRevisionConnectionProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.LibraryRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeLibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.GuaranteedLibraryConnectionProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.ExecutionException

private const val libraryId = 194
private const val serviceFileId = 31

class WhenGettingFileProperties {
	private val filePropertiesProvider by lazy {
		val fakeFileConnectionProvider = spyk<FakeRevisionConnectionProvider> {
			every { promiseResponse("File/GetInfo", "File=$serviceFileId") } returns Promise(IOException("Canceled"))
		}
        val fakeLibraryConnectionProvider =
            FakeLibraryConnectionProvider(mapOf(Pair(LibraryId(libraryId), fakeFileConnectionProvider)))
        FilePropertiesProvider(
			GuaranteedLibraryConnectionProvider(fakeLibraryConnectionProvider),
            LibraryRevisionProvider(fakeLibraryConnectionProvider),
            mockk(relaxed = true)
        )
    }

	private var ioException: IOException? = null

	@BeforeAll
	fun act() {
		try {
			filePropertiesProvider
				.promiseFileProperties(LibraryId(libraryId), ServiceFile(serviceFileId))
				.toExpiringFuture()
				.get()
		} catch (ee: ExecutionException) {
			ioException = ee.cause as? IOException
		}
	}

    @Test
    fun `then the exception is thrown`() {
        assertThat(ioException?.message).isEqualTo("Canceled")
    }
}
