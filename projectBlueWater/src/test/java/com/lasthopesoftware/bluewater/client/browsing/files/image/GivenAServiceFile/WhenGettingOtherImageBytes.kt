package com.lasthopesoftware.bluewater.client.browsing.files.image.GivenAServiceFile

import com.lasthopesoftware.bluewater.client.access.RemoteLibraryAccess
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnection
import com.lasthopesoftware.bluewater.shared.images.bytes.RemoteImageAccess
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenGettingOtherImageBytes {

	private val imageBytes by lazy {
//		val fakeConnectionProvider = FakeConnectionProvider()
//		fakeConnectionProvider.mapResponse(
//			{
//				FakeConnectionResponseTuple(
//					200,
//					byteArrayOf(46, 78, 99, 42)
//				)
//			},
//			"File/GetImage",
//			"File=583",
//			"Type=Full",
//			"Pad=1",
//			"Format=jpg",
//			"FillTransparency=ffffff"
//		)

		val memoryCachedImageAccess = RemoteImageAccess(
			mockk {
				every { promiseLibraryConnection(LibraryId(11)) } returns Promise(mockk<LiveServerConnection> {
					every { dataAccess } returns mockk<RemoteLibraryAccess> {
						every { promiseImageBytes(ServiceFile("583")) } returns byteArrayOf(46, 78, 99, 42).toPromise()
					}
				})
			})

		memoryCachedImageAccess.promiseImageBytes(LibraryId(11), ServiceFile("583")).toExpiringFuture().get()
	}

	@Test
	fun thenTheBytesAreCorrect() {
		assertThat(imageBytes).isEqualTo(byteArrayOf(46, 78, 99, 42))
	}
}
