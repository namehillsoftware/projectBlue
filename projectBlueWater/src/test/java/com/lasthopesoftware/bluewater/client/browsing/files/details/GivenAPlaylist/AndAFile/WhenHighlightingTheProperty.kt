package com.lasthopesoftware.bluewater.client.browsing.files.details.GivenAPlaylist.AndAFile

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FileProperty
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
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

private const val serviceFileId = 485

@RunWith(AndroidJUnit4::class)
class WhenHighlightingTheProperty {
	companion object {

		private var viewModel: Lazy<FileDetailsViewModel>? = lazy {
			FileDetailsViewModel(
				mockk {
					every { promiseIsReadOnly() } returns false.toPromise()
				},
				mockk {
					every { promiseFileProperties(ServiceFile(serviceFileId)) } returns Promise(
						sequenceOf(
							FileProperty(KnownFileProperties.Rating, "947"),
							FileProperty("sound", "wave"),
							FileProperty("creature", "concern"),
							FileProperty(KnownFileProperties.Name, "mat"),
							FileProperty(KnownFileProperties.Artist, "send"),
							FileProperty(KnownFileProperties.Genre, "rush"),
							FileProperty(KnownFileProperties.Lyrics, "reach"),
							FileProperty(KnownFileProperties.Comment, "police"),
							FileProperty(KnownFileProperties.Composer, "present"),
							FileProperty(KnownFileProperties.Custom, "steel"),
							FileProperty(KnownFileProperties.Publisher, "lipstick"),
							FileProperty(KnownFileProperties.TotalDiscs, "small"),
							FileProperty(KnownFileProperties.Track, "anxious"),
							FileProperty(KnownFileProperties.AlbumArtist, "date"),
							FileProperty(KnownFileProperties.Album, "ever"),
							FileProperty(KnownFileProperties.Date, "9"),
						)
					)
				},
				mockk(),
				mockk {
					every { promiseFileBitmap() } returns BitmapFactory
						.decodeByteArray(byteArrayOf(3, 4), 0, 2)
						.toPromise()
				},
				mockk {
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
			viewModel?.value?.apply {
				loadFromList(LibraryId(476), listOf(ServiceFile(serviceFileId)), 0).toExpiringFuture().get()
				fileProperties.value.first { it.property == KnownFileProperties.Publisher }.highlight()
			}
		}

		@JvmStatic
		@AfterClass
		fun cleanup() {
			viewModel = null
		}
	}

	@Test
	fun `then the highlighted property is correct`() {
		assertThat(viewModel?.value?.highlightedProperty?.value?.property).isEqualTo(KnownFileProperties.Publisher)
	}

	@Test
	fun `then the highlighted property value is correct`() {
		assertThat(viewModel?.value?.highlightedProperty?.value?.committedValue?.value).isEqualTo("lipstick")
	}
}
