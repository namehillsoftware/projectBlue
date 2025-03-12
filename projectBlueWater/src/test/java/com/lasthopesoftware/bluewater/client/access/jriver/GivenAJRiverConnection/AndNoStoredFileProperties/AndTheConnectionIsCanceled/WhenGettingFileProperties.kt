package com.lasthopesoftware.bluewater.client.access.jriver.GivenAJRiverConnection.AndNoStoredFileProperties.AndTheConnectionIsCanceled

import com.lasthopesoftware.bluewater.client.access.JRiverLibraryAccess
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.connection.ProvideConnections
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.ExecutionException

private const val serviceFileId = 31

class WhenGettingFileProperties {
	private val filePropertiesProvider by lazy {
		val fakeFileConnectionProvider = mockk<ProvideConnections> {
			every { promiseResponse("File/GetInfo", "File=$serviceFileId") } returns Promise(IOException("Canceled"))
		}

        JRiverLibraryAccess(fakeFileConnectionProvider)
    }

	private var ioException: IOException? = null

	@BeforeAll
	fun act() {
		try {
			filePropertiesProvider
				.promiseFileProperties(ServiceFile(serviceFileId))
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
