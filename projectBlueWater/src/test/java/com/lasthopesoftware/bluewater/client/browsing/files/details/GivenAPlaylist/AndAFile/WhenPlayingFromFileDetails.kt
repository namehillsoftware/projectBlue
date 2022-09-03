package com.lasthopesoftware.bluewater.client.browsing.files.details.GivenAPlaylist.AndAFile

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.image.ProvideImages
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FakeScopedCachedFilesPropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.images.ProvideDefaultImage
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

private const val serviceFileId = 338

private lateinit var startedList: List<ServiceFile>
private var startedPosition = -1

private val mut by lazy {
	val fakeFilesPropertiesProvider = FakeScopedCachedFilesPropertiesProvider().apply {
		addFilePropertiesToCache(
			ServiceFile(serviceFileId),
			mapOf(
				Pair(KnownFileProperties.NAME, "toward"),
				Pair(KnownFileProperties.ARTIST, "load"),
				Pair(KnownFileProperties.ALBUM, "square"),
				Pair(KnownFileProperties.RATING, "4"),
				Pair("razor", "through"),
				Pair("smile", "since"),
				Pair("harvest", "old"),
			)
		)
	}

	FileDetailsViewModel(
		fakeFilesPropertiesProvider,
		mockk<ProvideDefaultImage>().apply {
			every { promiseFileBitmap() } returns BitmapFactory
				.decodeByteArray(byteArrayOf(111, 112), 0, 2)
				.toPromise()
		},
		mockk<ProvideImages>().apply {
			every { promiseFileBitmap(any()) } returns BitmapFactory
				.decodeByteArray(byteArrayOf(322.toByte(), 480.toByte()), 0, 2)
				.toPromise()
		},
		mockk<ControlPlaybackService>().apply {
			every { startPlaylist(any<List<ServiceFile>>(), any()) } answers {
				startedList = firstArg()
				startedPosition = lastArg()
			}
		}
	)
}

@RunWith(AndroidJUnit4::class)
class WhenPlayingFromFileDetails {
	companion object {
		@BeforeClass
		@JvmStatic
		fun act(): Unit = with (mut) {
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
		assertThat(mut.artist.value).isEqualTo("load")
	}

	@Test
	fun `then the rating is correct`() {
		assertThat(mut.rating.value).isEqualTo(4)
	}

	@Test
	fun `then the file name is correct`() {
		assertThat(mut.fileName.value).isEqualTo("toward")
	}

	@Test
	fun `then the file properties are correct`() {
		assertThat(mut.fileProperties.value).containsExactlyInAnyOrder(
			*(mapOf(
				Pair(KnownFileProperties.NAME, "toward"),
				Pair(KnownFileProperties.ARTIST, "load"),
				Pair(KnownFileProperties.ALBUM, "square"),
				Pair(KnownFileProperties.RATING, "4"),
				Pair("razor", "through"),
				Pair("smile", "since"),
				Pair("harvest", "old"),
			).entries.toTypedArray())
		)
	}
}
