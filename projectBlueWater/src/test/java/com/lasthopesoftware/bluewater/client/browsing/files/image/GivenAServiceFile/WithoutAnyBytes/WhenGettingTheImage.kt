package com.lasthopesoftware.bluewater.client.browsing.files.image.GivenAServiceFile.WithoutAnyBytes

import android.graphics.Bitmap
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.image.ImageProvider
import com.lasthopesoftware.bluewater.client.browsing.files.image.bytes.GetRawImages
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.AfterClass
import org.junit.Test

class WhenGettingTheImage : AndroidContext() {

	companion object {
		private var bitmap: Bitmap? = null

		@AfterClass
		@JvmStatic
		fun cleanup() {
			bitmap = null
		}
	}

    override fun before() {
		val selectedLibraryProvider = mockk<ProvideSelectedLibraryId>()
		every { selectedLibraryProvider.selectedLibraryId } returns Promise(LibraryId(2))

		val getRawImages = mockk<GetRawImages>()
		every { getRawImages.promiseImageBytes(LibraryId(2), ServiceFile(34)) } returns Promise(ByteArray(0))

        val imageProvider = ImageProvider(
            selectedLibraryProvider,
            getRawImages)
        bitmap = imageProvider.promiseFileBitmap(ServiceFile(34)).toExpiringFuture().get()
    }

    @Test
    fun thenTheBitmapIsNull() {
        assertThat(bitmap).isNull()
    }
}
