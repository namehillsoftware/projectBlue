package com.lasthopesoftware.bluewater.client.browsing.files.details.GivenAPlaylist.AndAFile.AndTheConnectionIsReadOnly

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.properties.EditableFilePropertyDefinition
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertyType
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.connection.libraries.PassThroughScopedUrlKeyProvider
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

private const val serviceFileId = 220

@RunWith(AndroidJUnit4::class)
class WhenAnotherPropertyIsEdited {
	companion object {
		private var persistedValue = ""

		private var viewModel: Lazy<FileDetailsViewModel>? = lazy {
			FileDetailsViewModel(
				mockk {
					every { promiseIsReadOnly() } returns true.toPromise()
				},
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
							Pair(KnownFileProperties.Track, "882"),
							Pair(KnownFileProperties.AlbumArtist, "calm"),
							Pair(KnownFileProperties.Album, "distant"),
							Pair(KnownFileProperties.Date, "1355"),
							Pair(KnownFileProperties.Band, "stair"),
						)
					)
				},
				mockk {
					every { promiseFileUpdate(ServiceFile(serviceFileId), KnownFileProperties.Custom, any(), false) } answers {
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

		private val FileDetailsViewModel.propertyToEdit
			get() = fileProperties.value.first { it.property == EditableFilePropertyDefinition.Band.descriptor }

		@JvmStatic
		@BeforeClass
		fun act() {
			viewModel?.value?.apply {
				loadFromList(listOf(ServiceFile(serviceFileId)), 0).toExpiringFuture().get()
				propertyToEdit.apply {
					highlight()
					edit()
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
	fun `then the property has the correct editable type`() {
		assertThat(viewModel?.value?.propertyToEdit?.editableType).isEqualTo(FilePropertyType.ShortFormText)
	}

	@Test
	fun `then the property is NOT being edited`() {
		assertThat(viewModel?.value?.propertyToEdit?.isEditing?.value).isFalse
	}

	@Test
	fun `then the new property is highlighted`() {
		assertThat(viewModel?.value?.highlightedProperty?.value).isEqualTo(viewModel?.value?.propertyToEdit)
	}
}
