package com.lasthopesoftware.bluewater.client.browsing.files.image.GivenAServiceFile.WithoutAnImage

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionResponseTuple
import com.lasthopesoftware.bluewater.client.connection.FakeLibraryConnectionProvider
import com.lasthopesoftware.bluewater.shared.images.bytes.RemoteImageAccess
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenGettingTheImageBytes {
	private val imageBytes by lazy {
		val fakeConnectionProvider = FakeConnectionProvider()
		fakeConnectionProvider.mapResponse(
			{
				FakeConnectionResponseTuple(
					500,
					"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>\r\n<Response Status=\"Failure\"/>\r\n".toByteArray()
				)
			},
			"File/GetImage",
			"File=31",
			"Type=Full",
			"Pad=1",
			"Format=jpg",
			"FillTransparency=ffffff"
		)
		val memoryCachedImageAccess = RemoteImageAccess(
			FakeLibraryConnectionProvider(mapOf(Pair(LibraryId(21), fakeConnectionProvider))))

		memoryCachedImageAccess.promiseImageBytes(LibraryId(21), ServiceFile(31)).toExpiringFuture().get()
	}

	@Test
	fun thenTheBytesAreEmpty() {
		assertThat(imageBytes).isEmpty()
	}
}
