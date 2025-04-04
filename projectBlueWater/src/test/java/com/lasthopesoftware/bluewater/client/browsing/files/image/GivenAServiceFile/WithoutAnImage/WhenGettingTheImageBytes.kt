package com.lasthopesoftware.bluewater.client.browsing.files.image.GivenAServiceFile.WithoutAnImage

import com.lasthopesoftware.bluewater.client.access.RemoteLibraryAccess
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnection
import com.lasthopesoftware.bluewater.shared.images.bytes.RemoteImageAccess
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.FileNotFoundException

class WhenGettingTheImageBytes {
	private val imageBytes by lazy {
		val imageAccess = RemoteImageAccess(
			mockk {
				every { promiseLibraryConnection(LibraryId(21)) } returns Promise(mockk<LiveServerConnection> {
					every { dataAccess } returns mockk<RemoteLibraryAccess> {
						every { promiseImageBytes(ServiceFile("31")) } returns Promise(FileNotFoundException())
					}
				})
			})

		imageAccess.promiseImageBytes(LibraryId(21), ServiceFile("31")).toExpiringFuture().get()
	}

	@Test
	fun thenTheBytesAreEmpty() {
		assertThat(imageBytes).isEmpty()
	}
}
