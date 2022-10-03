package com.lasthopesoftware.bluewater.client.browsing.files.details.GivenAPlaylist.AndAFile.AndTheFileIsLoaded

import android.graphics.BitmapFactory
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.TypedFileProperty
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.URL

private const val serviceFileId = 79

private val viewModel by lazy {
	FileDetailsViewModel(
		mockk {
			every { promiseFileProperties(ServiceFile(serviceFileId)) } returns Promise(
				mapOf(
					Pair(KnownFileProperties.Rating, "2"),
					Pair("awkward", "prevent"),
					Pair("feast", "wind"),
					Pair(KnownFileProperties.Name, "please"),
					Pair(KnownFileProperties.Artist, "brown"),
					Pair(KnownFileProperties.Genre, "subject")
				)
			)
		},
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
			every { promiseUrlKey(ServiceFile(serviceFileId)) } returns UrlKeyHolder(
				URL("http://bow"), ServiceFile(serviceFileId)).toPromise()
		},
	)
}

@RunWith(RobolectricTestRunner::class)
class WhenEditingTheFileProperties {
	companion object {
		private var editingFileProperties: Map<TypedFileProperty, String>? = null

		@JvmStatic
		@BeforeClass
		fun act() {
			viewModel.loadFromList(
				listOf(
					ServiceFile(53),
					ServiceFile(926),
					ServiceFile(145),
					ServiceFile(668),
					ServiceFile(serviceFileId),
					ServiceFile(360),
					ServiceFile(771),
					ServiceFile(304),
					ServiceFile(651),
				),
				4).toExpiringFuture().get()
			editingFileProperties = viewModel.editFileProperties()
		}
	}

	@Test
	fun `then the view model is editing`() {
		assertThat(viewModel.isEditing.value).isTrue
	}

	@Test
	fun `then the properties can be edited`() {
		assertThat(editingFileProperties).containsAllEntriesOf(
			mapOf(
				Pair(TypedFileProperty.Rating, "2"),
				Pair(TypedFileProperty.Name, "please"),
				Pair(TypedFileProperty.Artist, "brown"),
				Pair(TypedFileProperty.Genre, "subject"),
			)
		)
	}
}
