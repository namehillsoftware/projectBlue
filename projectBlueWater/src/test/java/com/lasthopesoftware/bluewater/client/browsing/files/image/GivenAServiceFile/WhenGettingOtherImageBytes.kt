package com.lasthopesoftware.bluewater.client.browsing.files.image.GivenAServiceFile

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionResponseTuple
import com.lasthopesoftware.bluewater.client.connection.FakeLibraryConnectionProvider
import com.lasthopesoftware.bluewater.shared.images.bytes.RemoteImageAccess
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenGettingOtherImageBytes {

	private val imageBytes by lazy {
		val fakeConnectionProvider = FakeConnectionProvider()
		fakeConnectionProvider.mapResponse(
			{
				FakeConnectionResponseTuple(
					200,
					byteArrayOf(46, 78, 99, 42)
				)
			},
			"File/GetImage",
			"File=583",
			"Type=Full",
			"Pad=1",
			"Format=jpg",
			"FillTransparency=ffffff"
		)

		val memoryCachedImageAccess = RemoteImageAccess(
			FakeLibraryConnectionProvider(mapOf(Pair(LibraryId(11), fakeConnectionProvider))))

		memoryCachedImageAccess.promiseImageBytes(LibraryId(11), ServiceFile(583)).toExpiringFuture().get()
	}

	@Test
	fun thenTheBytesAreCorrect() {
		assertThat(imageBytes).isEqualTo(byteArrayOf(46, 78, 99, 42))
	}
}
