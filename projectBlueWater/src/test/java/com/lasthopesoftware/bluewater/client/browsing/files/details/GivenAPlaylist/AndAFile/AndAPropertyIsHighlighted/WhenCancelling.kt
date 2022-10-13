package com.lasthopesoftware.bluewater.client.browsing.files.details.GivenAPlaylist.AndAFile.AndAPropertyIsHighlighted

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

private const val serviceFileId = 300

@RunWith(AndroidJUnit4::class)
class WhenHighlightingTheProperty {
	companion object {

		private var viewModel: Lazy<FileDetailsViewModel>? = lazy {
			FileDetailsViewModel(
				mockk {
					every { promiseFileProperties(ServiceFile(serviceFileId)) } returns Promise(
						mapOf(
							Pair(KnownFileProperties.Rating, "412"),
							Pair("simple", "middle"),
							Pair("aside", "vessel"),
							Pair(KnownFileProperties.Name, "skin"),
							Pair(KnownFileProperties.Artist, "afford"),
							Pair(KnownFileProperties.Genre, "avenue"),
							Pair(KnownFileProperties.Lyrics, "regret"),
							Pair(KnownFileProperties.Comment, "dream"),
							Pair(KnownFileProperties.Composer, "risk"),
							Pair(KnownFileProperties.Custom, "fate"),
							Pair(KnownFileProperties.Publisher, "crash"),
							Pair(KnownFileProperties.TotalDiscs, "bone"),
							Pair(KnownFileProperties.Track, "passage"),
							Pair(KnownFileProperties.AlbumArtist, "enclose"),
							Pair(KnownFileProperties.Album, "amuse"),
							Pair(KnownFileProperties.Date, "9357"),
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
				fileProperties.value.first { it.property == KnownFileProperties.Date }.apply {
					highlight()
					cancel()
				}
			}
		}

		@JvmStatic
		@AfterClass
		fun cleanup() {
			viewModel = null
		}
	}

	@Test
	fun `then there is no highlighted property`() {
		assertThat(viewModel?.value?.highlightedProperty?.value).isNull()
	}
}
