package com.lasthopesoftware.bluewater.client.browsing.files.details.GivenAPlaylist.AndAFile

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FakeScopedCachedFilesPropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL

private const val serviceFileId = 338

private lateinit var startedList: List<ServiceFile>
private var startedPosition = -1

@RunWith(AndroidJUnit4::class)
class WhenPlayingFromFileDetails {
	companion object {

		private var mut: Lazy<FileDetailsViewModel>? = lazy {
			val fakeFilesPropertiesProvider = FakeScopedCachedFilesPropertiesProvider().apply {
				addFilePropertiesToCache(
					ServiceFile(serviceFileId),
					mapOf(
						Pair(KnownFileProperties.Name, "toward"),
						Pair(KnownFileProperties.Artist, "load"),
						Pair(KnownFileProperties.Album, "square"),
						Pair(KnownFileProperties.Rating, "4"),
						Pair("razor", "through"),
						Pair("smile", "since"),
						Pair("harvest", "old"),
					)
				)
			}

			FileDetailsViewModel(
				mockk {
					every { promiseIsReadOnly() } returns false.toPromise()
				},
				fakeFilesPropertiesProvider,
				mockk(),
				mockk {
					every { promiseFileBitmap() } returns BitmapFactory
						.decodeByteArray(byteArrayOf(111, 112), 0, 2)
						.toPromise()
				},
				mockk {
					every { promiseFileBitmap(any()) } returns BitmapFactory
						.decodeByteArray(byteArrayOf(322.toByte(), 480.toByte()), 0, 2)
						.toPromise()
				},
				mockk {
					every { startPlaylist(any<List<ServiceFile>>(), any()) } answers {
						startedList = firstArg()
						startedPosition = lastArg()
					}
				},
				RecordingApplicationMessageBus(),
				mockk {
					every { promiseUrlKey(ServiceFile(serviceFileId)) } returns UrlKeyHolder(URL("http://bow"), ServiceFile(serviceFileId)).toPromise()
				},
			)
		}

		@BeforeClass
		@JvmStatic
		fun act(): Unit = mut?.value?.run {
			loadFromList(
				listOf(
					ServiceFile(830),
					ServiceFile(serviceFileId),
					ServiceFile(628),
					ServiceFile(537),
					ServiceFile(284),
					ServiceFile(419),
					ServiceFile(36),
					ServiceFile(396),
				),
				1
			).toExpiringFuture().get()

			play()
		} ?: Unit

		@AfterClass
		@JvmStatic
		fun cleanup() {
			mut = null
		}
	}

	@Test
	fun `then the correct playlist is started`() {
		assertThat(startedList).containsExactlyInAnyOrder(
			ServiceFile(830),
			ServiceFile(serviceFileId),
			ServiceFile(628),
			ServiceFile(537),
			ServiceFile(284),
			ServiceFile(419),
			ServiceFile(36),
			ServiceFile(396),
		)
	}

	@Test
	fun `then the playlist is started at the correct position`() {
		assertThat(startedPosition).isEqualTo(1)
	}

	@Test
	fun `then the artist is correct`() {
		assertThat(mut?.value?.artist?.value).isEqualTo("load")
	}

	@Test
	fun `then the rating is correct`() {
		assertThat(mut?.value?.rating?.value).isEqualTo(4)
	}

	@Test
	fun `then the file name is correct`() {
		assertThat(mut?.value?.fileName?.value).isEqualTo("toward")
	}

	@Test
	fun `then the file properties are correct`() {
		assertThat(mut?.value?.fileProperties?.value?.map { Pair(it.property, it.committedValue.value) }).containsExactlyInAnyOrder(
			Pair(KnownFileProperties.Name, "toward"),
			Pair(KnownFileProperties.Artist, "load"),
			Pair(KnownFileProperties.Album, "square"),
			Pair(KnownFileProperties.Rating, "4"),
			Pair("razor", "through"),
			Pair("smile", "since"),
			Pair("harvest", "old"),
		)
	}
}
