package com.lasthopesoftware.bluewater.client.browsing.files.details.GivenAServiceFile.AndTheFileIsLoaded

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.image.ProvideImages
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideScopedFileProperties
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
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

private const val serviceFileId = 860

private var addedServiceFile: ServiceFile? = null

private val viewModel by lazy {
	FileDetailsViewModel(
		mockk<ProvideScopedFileProperties>().apply {
			every { promiseFileProperties(ServiceFile(serviceFileId)) } returns Promise(emptyMap())
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
		mockk<ControlPlaybackService>().apply {
			every { addToPlaylist(any()) } answers {
				addedServiceFile = firstArg()
			}
		}
	)
}

@RunWith(AndroidJUnit4::class)
class WhenAddingTheFileToNowPlaying {
	companion object {
		@JvmStatic
		@BeforeClass
		fun act() {
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
			viewModel.addToNowPlaying()
		}
	}

	@Test
	fun `then the file is added to now playing`() {
		assertThat(addedServiceFile).isEqualTo(ServiceFile(serviceFileId))
	}
}