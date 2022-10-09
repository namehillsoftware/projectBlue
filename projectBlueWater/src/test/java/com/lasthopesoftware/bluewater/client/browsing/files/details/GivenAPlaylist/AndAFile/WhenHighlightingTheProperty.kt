package com.lasthopesoftware.bluewater.client.browsing.files.details.GivenAPlaylist.AndAFile

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
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

private const val serviceFileId = 485

@RunWith(AndroidJUnit4::class)
class WhenHighlightingTheProperty {
	companion object {

		private var viewModel: Lazy<FileDetailsViewModel>? = lazy {
			FileDetailsViewModel(
				mockk {
					every { promiseFileProperties(ServiceFile(serviceFileId)) } returns Promise(
						mapOf(
							Pair(KnownFileProperties.Rating, "947"),
							Pair("sound", "wave"),
							Pair("creature", "concern"),
							Pair(KnownFileProperties.Name, "mat"),
							Pair(KnownFileProperties.Artist, "send"),
							Pair(KnownFileProperties.Genre, "rush"),
							Pair(KnownFileProperties.Lyrics, "reach"),
							Pair(KnownFileProperties.Comment, "police"),
							Pair(KnownFileProperties.Composer, "present"),
							Pair(KnownFileProperties.Custom, "steel"),
							Pair(KnownFileProperties.Publisher, "lipstick"),
							Pair(KnownFileProperties.TotalDiscs, "small"),
							Pair(KnownFileProperties.Track, "anxious"),
							Pair(KnownFileProperties.AlbumArtist, "date"),
							Pair(KnownFileProperties.Album, "ever"),
							Pair(KnownFileProperties.Date, "9"),
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
				loadFromList(listOf(ServiceFile(serviceFileId)), 0).toExpiringFuture().get()
				highlightProperty(KnownFileProperties.Publisher)
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
		assertThat(viewModel?.value?.highlightedProperty?.value).isEqualTo(Pair(KnownFileProperties.Publisher, "lipstick"))
	}
}
