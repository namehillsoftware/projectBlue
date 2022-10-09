package com.lasthopesoftware.bluewater.client.browsing.files.details.GivenAPlaylist.AndAFile.AndAFilePropertiesUpdateMessageIsBroadcastForADifferentFile

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertiesUpdatedMessage
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

private const val serviceFileId = 783

@RunWith(AndroidJUnit4::class)
class WhenTheFilePropertiesChange {
	companion object {

		private var services: Lazy<Pair<RecordingApplicationMessageBus, FileDetailsViewModel>>? = lazy {
			val recordingApplicationMessageBus = RecordingApplicationMessageBus()

			Pair(
				recordingApplicationMessageBus,
				FileDetailsViewModel(
					mockk {
						every { promiseFileProperties(ServiceFile(serviceFileId)) } returns Promise(
							mapOf(
								Pair(KnownFileProperties.Rating, "815"),
								Pair("little", "more"),
								Pair("evening", "skin"),
								Pair(KnownFileProperties.Name, "ahead"),
								Pair(KnownFileProperties.Artist, "moon"),
								Pair(KnownFileProperties.Album, "number"),
								Pair(KnownFileProperties.ImageFile, "battle"),
							)
						) andThen Promise(
							mapOf(
								Pair(KnownFileProperties.Rating, "7"),
								Pair("bread", "scenery"),
								Pair("rush", "offense"),
								Pair(KnownFileProperties.Name, "kiss"),
								Pair(KnownFileProperties.Artist, "adoption"),
								Pair(KnownFileProperties.Album, "motherly"),
								Pair(KnownFileProperties.StackTop, "under"),
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
					recordingApplicationMessageBus,
					mockk {
						every { promiseUrlKey(ServiceFile(serviceFileId)) } returns UrlKeyHolder(URL("http://bow"), ServiceFile(serviceFileId)).toPromise()
					}
				)
			)
		}

		@JvmStatic
		@BeforeClass
		fun act() {
			val (messageBus, viewModel) = services?.value ?: return

			viewModel.loadFromList(
				listOf(
					ServiceFile(291),
					ServiceFile(312),
					ServiceFile(783),
					ServiceFile(380),
					ServiceFile(serviceFileId),
					ServiceFile(723),
					ServiceFile(81),
					ServiceFile(543),
				),
				4
			).toExpiringFuture().get()

			messageBus.sendMessage(FilePropertiesUpdatedMessage(UrlKeyHolder(URL("http://bow"), ServiceFile(937))))
		}

		@JvmStatic
		@AfterClass
		fun cleanup() {
			services = null
		}
	}

	@Test
	fun `then the properties are correct`() {
		assertThat(services?.value?.second?.fileProperties?.value?.entries).hasSameElementsAs(
			mapOf(
				Pair(KnownFileProperties.Rating, "815"),
				Pair("little", "more"),
				Pair("evening", "skin"),
				Pair(KnownFileProperties.Name, "ahead"),
				Pair(KnownFileProperties.Artist, "moon"),
				Pair(KnownFileProperties.Album, "number"),
			).entries
		)
	}

	@Test
	fun `then the rating is correct`() {
		assertThat(services?.value?.second?.rating?.value).isEqualTo(815)
	}

	@Test
	fun `then the artist is correct`() {
		assertThat(services?.value?.second?.artist?.value).isEqualTo("moon")
	}

	@Test
	fun `then the file name is correct`() {
		assertThat(services?.value?.second?.fileName?.value).isEqualTo("ahead")
	}

	@Test
	fun `then the album is correct`() {
		assertThat(services?.value?.second?.album?.value).isEqualTo("number")
	}
}
