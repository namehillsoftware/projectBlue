package com.lasthopesoftware.bluewater.client.browsing.files.details.GivenAPlaylist.AndAFile

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.image.ProvideImages
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideScopedFileProperties
import com.lasthopesoftware.bluewater.shared.images.ProvideDefaultImage
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

private const val serviceFileId = 161

private val viewModel by lazy {
	FileDetailsViewModel(
		mockk<ProvideScopedFileProperties>().apply {
			 every { promiseFileProperties(ServiceFile(serviceFileId)) } returns Promise(
				 mapOf(
					 Pair(KnownFileProperties.RATING, "3"),
					 Pair("too", "prevent"),
					 Pair("shirt", "wind"),
					 Pair(KnownFileProperties.NAME, "holiday"),
					 Pair(KnownFileProperties.ARTIST, "board"),
				 )
			 )
		},
		mockk<ProvideDefaultImage>().apply {
			every { promiseFileBitmap() } returns BitmapFactory
				.decodeByteArray(byteArrayOf(3, 4), 0, 2)
				.toPromise()
		},
		mockk<ProvideImages>().apply {
			every { promiseFileBitmap(any()) } returns BitmapFactory
				.decodeByteArray(byteArrayOf(61, 127), 0, 2)
				.toPromise()
		},
		mockk(),
	)
}

@RunWith(AndroidJUnit4::class)
class WhenLoading {
	companion object {
		@JvmStatic
		@BeforeClass
		fun act() {
			viewModel.loadFromList(listOf(ServiceFile(serviceFileId)), 0).toExpiringFuture().get()
		}
	}

	@Test
	fun `then the properties are correct`() {
		assertThat(viewModel.fileProperties.value).hasSameElementsAs(
			mapOf(
				Pair(KnownFileProperties.RATING, "3"),
				Pair("too", "prevent"),
				Pair("shirt", "wind"),
				Pair(KnownFileProperties.NAME, "holiday"),
				Pair(KnownFileProperties.ARTIST, "board"),
			).entries
		)
	}

	@Test
	fun `then the rating is correct`() {
		assertThat(viewModel.rating.value).isEqualTo(3)
	}

	@Test
	fun `then the artist is correct`() {
		assertThat(viewModel.artist.value).isEqualTo("board")
	}

	@Test
	fun `then the file name is correct`() {
		assertThat(viewModel.fileName.value).isEqualTo("holiday")
	}
}
