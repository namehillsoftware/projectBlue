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

private const val serviceFileId = 479
private val propertyBeingEdited = EditableFilePropertyDefinition.Comment
private val propertyEditedLate = EditableFilePropertyDefinition.Publisher
private var persistedValue = ""

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
					Pair(KnownFileProperties.Comment, "warn"),
					Pair(KnownFileProperties.Composer, "hotel"),
					Pair(KnownFileProperties.Custom, "curl"),
					Pair(KnownFileProperties.Publisher, "absolute"),
					Pair(KnownFileProperties.TotalDiscs, "354"),
					Pair(KnownFileProperties.Track, "703"),
					Pair(KnownFileProperties.AlbumArtist, "calm"),
					Pair(KnownFileProperties.Album, "distant"),
					Pair(KnownFileProperties.Date, "1355"),
				)
			)
		},
		mockk {
			every { promiseFileUpdate(ServiceFile(serviceFileId), any(), any(), false) } answers {
				persistedValue = arg(2)
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
class WhenSavingAndStoppingTheEdit {
	companion object {
		@JvmStatic
		@BeforeClass
		fun act() {
			viewModel.loadFromList(listOf(ServiceFile(serviceFileId)), 0).toExpiringFuture().get()
			viewModel.editFileProperties()
			viewModel.editFileProperty(propertyBeingEdited).toExpiringFuture().get()
			viewModel.editableFileProperty.value?.updateValue("possible")
			viewModel.saveAndStopEditing().toExpiringFuture().get()
			viewModel.editFileProperty(propertyEditedLate)
			viewModel.editableFileProperty.value?.run {
				updateValue("next")
				commitChanges().toExpiringFuture().get()
			}
		}
	}

	@Test
	fun `then the view model is NOT editing`() {
		assertThat(viewModel.isEditing.value).isFalse
	}

	@Test
	fun `then the property that is edited too late is NOT changed`() {
		assertThat(viewModel.fileProperties.value[propertyEditedLate.descriptor]).isEqualTo("absolute")
	}

	@Test
	fun `then the property change is persisted`() {
		assertThat(persistedValue).isEqualTo("possible")
	}

	@Test
	fun `then the editable file property is null`() {
		assertThat(viewModel.editableFileProperty.value).isNull()
	}
}
