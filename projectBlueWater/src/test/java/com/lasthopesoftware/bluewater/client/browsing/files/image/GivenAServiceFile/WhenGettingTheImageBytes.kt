package com.lasthopesoftware.bluewater.client.browsing.files.image.GivenAServiceFile

import com.lasthopesoftware.bluewater.client.access.RemoteLibraryAccess
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnection
import com.lasthopesoftware.bluewater.shared.images.bytes.RemoteImageAccess
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.ProgressingPromise
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenGettingTheImageBytes {
	private val imageBytes by lazy {
//		val fakeConnectionProvider = FakeConnectionProvider()
//		fakeConnectionProvider.mapResponse(
//			{ FakeConnectionResponseTuple(200, byteArrayOf(39, 127, 8)) },
//			"File/GetImage",
//			"File=31",
//			"Type=Full",
//			"Pad=1",
//			"Format=jpg",
//			"FillTransparency=ffffff"
//		)

		val memoryCachedImageAccess = RemoteImageAccess(
			mockk {
				every { promiseLibraryConnection(LibraryId(21)) } returns ProgressingPromise(mockk<LiveServerConnection> {
					every { dataAccess } returns mockk<RemoteLibraryAccess> {
						every { promiseImageBytes(ServiceFile(31)) } returns byteArrayOf(39, 127, 8).toPromise()
					}
				})
			})

		memoryCachedImageAccess.promiseImageBytes(LibraryId(21), ServiceFile(31)).toExpiringFuture().get()
	}

	@Test
	fun thenTheBytesAreCorrect() {
		assertThat(imageBytes).isEqualTo(byteArrayOf(39, 127, 8))
	}
}
