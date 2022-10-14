package com.lasthopesoftware.bluewater.client.browsing.files.details.GivenAPlaylist.AndAFile

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.image.ProvideImages
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideScopedFileProperties
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.images.ProvideDefaultImage
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL

private const val serviceFileId = 161

@RunWith(AndroidJUnit4::class)
class WhenLoading {
	companion object {

		private var viewModel: Lazy<FileDetailsViewModel>? = lazy {
			FileDetailsViewModel(
				mockk<ProvideScopedFileProperties>().apply {
					every { promiseFileProperties(ServiceFile(serviceFileId)) } returns Promise(
						mapOf(
							Pair(KnownFileProperties.Rating, "3"),
							Pair("too", "prevent"),
							Pair("shirt", "wind"),
							Pair(KnownFileProperties.Name, "holiday"),
							Pair(KnownFileProperties.Artist, "board"),
							Pair(KnownFileProperties.Album, "virtue"),
						)
					)
				},
				mockk(),
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
				RecordingApplicationMessageBus(),
				mockk {
					every { promiseUrlKey(ServiceFile(serviceFileId)) } returns UrlKeyHolder(URL("http://bow"), ServiceFile(serviceFileId)).toPromise()
				},
			)
		}

		@JvmStatic
		@BeforeClass
		fun act() {
			viewModel?.value?.loadFromList(listOf(ServiceFile(serviceFileId)), 0)?.toExpiringFuture()?.get()
		}

		@JvmStatic
		@AfterClass
		fun cleanup() {
			viewModel = null
		}
	}

	@Test
	fun `then the properties are correct`() {
		assertThat(viewModel?.value?.fileProperties?.value?.map { Pair(it.property, it.committedValue.value) }).hasSameElementsAs(
			listOf(
				Pair(KnownFileProperties.Rating, "3"),
				Pair("too", "prevent"),
				Pair("shirt", "wind"),
				Pair(KnownFileProperties.Name, "holiday"),
				Pair(KnownFileProperties.Artist, "board"),
				Pair(KnownFileProperties.Album, "virtue"),
			)
		)
	}

	@Test
	fun `then the rating is correct`() {
		assertThat(viewModel?.value?.rating?.value).isEqualTo(3)
	}

	@Test
	fun `then the artist is correct`() {
		assertThat(viewModel?.value?.artist?.value).isEqualTo("board")
	}

	@Test
	fun `then the file name is correct`() {
		assertThat(viewModel?.value?.fileName?.value).isEqualTo("holiday")
	}

	@Test
	fun `then the album is correct`() {
		assertThat(viewModel?.value?.album?.value).isEqualTo("virtue")
	}
}
