package com.lasthopesoftware.bluewater.client.browsing.files.details.GivenAPlaylist.AndAFile.AndTheFileIsLoaded.AndThePropertiesAreBeingEdited.AndAPropertyIsModified

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.properties.EditableFilePropertyDefinition
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.connection.libraries.PassThroughScopedUrlKeyProvider
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
import java.net.URL

private const val serviceFileId = 79

private var persistedTrackNumber = ""

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
					Pair(KnownFileProperties.Genre, "subject"),
					Pair(KnownFileProperties.Lyrics, "belief"),
					Pair(KnownFileProperties.Comment, "pad"),
					Pair(KnownFileProperties.Composer, "hotel"),
					Pair(KnownFileProperties.Custom, "curl"),
					Pair(KnownFileProperties.Publisher, "capital"),
					Pair(KnownFileProperties.TotalDiscs, "354"),
					Pair(KnownFileProperties.Track, "703"),
					Pair(KnownFileProperties.AlbumArtist, "calm"),
					Pair(KnownFileProperties.Album, "distant"),
					Pair(KnownFileProperties.Date, "1355"),
				)
			)
		},
		mockk {
			every { promiseFileUpdate(ServiceFile(serviceFileId), KnownFileProperties.Track, any(), false) } answers {
				persistedTrackNumber = arg(2)
				Unit.toPromise()
			}
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
		PassThroughScopedUrlKeyProvider(URL("http://damage")),
	)
}

 @RunWith(AndroidJUnit4::class)
class WhenCancelling {
	companion object {
		@JvmStatic
		@BeforeClass
		fun act() {
			viewModel.loadFromList(listOf(ServiceFile(serviceFileId)), 0).toExpiringFuture().get()
			viewModel.editFileProperties()
			viewModel.editFileProperty(EditableFilePropertyDefinition.Track)
			viewModel.editableFileProperty.value?.run {
				updateValue("141")
				cancel()
			}
		}
	}

	@Test
	fun `then the view model is editing`() {
		assertThat(viewModel.isEditing.value).isTrue
	}

	@Test
	fun `then the property is NOT changed`() {
		assertThat(viewModel.fileProperties.value[KnownFileProperties.Track]).isEqualTo("703")
	}

	@Test
	fun `then the property change is NOT persisted`() {
		assertThat(persistedTrackNumber).isEmpty()
	}

	@Test
	fun `then the editable file property is null`() {
		assertThat(viewModel.editableFileProperty.value).isNull()
	}
}
