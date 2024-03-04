package com.lasthopesoftware.bluewater.client.browsing.files.details.GivenAPlaylist.AndAFile.AndAPropertyIsHighlighted

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

private const val serviceFileId = 300

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
							FileProperty(KnownFileProperties.Rating, "412"),
							FileProperty("simple", "middle"),
							FileProperty("aside", "vessel"),
							FileProperty(KnownFileProperties.Name, "skin"),
							FileProperty(KnownFileProperties.Artist, "afford"),
							FileProperty(KnownFileProperties.Genre, "avenue"),
							FileProperty(KnownFileProperties.Lyrics, "regret"),
							FileProperty(KnownFileProperties.Comment, "dream"),
							FileProperty(KnownFileProperties.Composer, "risk"),
							FileProperty(KnownFileProperties.Custom, "fate"),
							FileProperty(KnownFileProperties.Publisher, "crash"),
							FileProperty(KnownFileProperties.TotalDiscs, "bone"),
							FileProperty(KnownFileProperties.Track, "passage"),
							FileProperty(KnownFileProperties.AlbumArtist, "enclose"),
							FileProperty(KnownFileProperties.Album, "amuse"),
							FileProperty(KnownFileProperties.Date, "9357"),
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
				loadFromList(LibraryId(738), listOf(ServiceFile(serviceFileId)), 0).toExpiringFuture().get()
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
