package com.lasthopesoftware.bluewater.client.browsing.files.details.GivenAPlaylist.AndAFile.AndThePropertiesAreBeingEdited

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FileProperty
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertyType
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.PassThroughUrlKeyProvider
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

private const val libraryId = 805
private const val serviceFileId = 220

@RunWith(AndroidJUnit4::class)
class WhenAnotherPropertyIsEdited {
	companion object {
		private var persistedValue = ""

		private var viewModel: Lazy<FileDetailsViewModel>? = lazy {
			FileDetailsViewModel(
				mockk {
					every { promiseIsReadOnly(LibraryId(libraryId)) } returns false.toPromise()
				},
				mockk {
					every { promiseFileProperties(LibraryId(libraryId), ServiceFile(serviceFileId)) } returns Promise(
						sequenceOf(
							FileProperty(KnownFileProperties.Rating, "2"),
							FileProperty("awkward", "prevent"),
							FileProperty("feast", "wind"),
							FileProperty(KnownFileProperties.Name, "please"),
							FileProperty(KnownFileProperties.Artist, "brown"),
							FileProperty(KnownFileProperties.Genre, "subject"),
							FileProperty(KnownFileProperties.Lyrics, "belief"),
							FileProperty(KnownFileProperties.Comment, "pad"),
							FileProperty(KnownFileProperties.Composer, "hotel"),
							FileProperty(KnownFileProperties.Custom, "curl"),
							FileProperty(KnownFileProperties.Publisher, "capital"),
							FileProperty(KnownFileProperties.TotalDiscs, "354"),
							FileProperty(KnownFileProperties.Track, "882"),
							FileProperty(KnownFileProperties.AlbumArtist, "calm"),
							FileProperty(KnownFileProperties.Album, "distant"),
							FileProperty(KnownFileProperties.Date, "1355"),
						)
					)
				},
				mockk {
					every { promiseFileUpdate(LibraryId(libraryId), ServiceFile(serviceFileId), KnownFileProperties.Custom, any(), false) } answers {
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
					every { promiseFileBitmap(LibraryId(libraryId), any()) } returns BitmapFactory
						.decodeByteArray(byteArrayOf(61, 127), 0, 2)
						.toPromise()
				},
				mockk(),
				RecordingApplicationMessageBus(),
				PassThroughUrlKeyProvider(URL("http://damage")),
			)
		}

		@JvmStatic
		@BeforeClass
		fun act() {
			viewModel?.value?.apply {
				loadFromList(LibraryId(libraryId), listOf(ServiceFile(serviceFileId)), 0).toExpiringFuture().get()
				fileProperties.apply {
					value.first { it.property == KnownFileProperties.Custom }
						.apply {
							highlight()
							edit()
							updateValue("omit")
						}
					value.first { it.property == KnownFileProperties.AlbumArtist }.apply {
						highlight()
						edit()
						updateValue("silk")
					}
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
	fun `then the property change is not persisted`() {
		assertThat(persistedValue).isEmpty()
	}

	@Test
	fun `then the original property is not being edited`() {
		assertThat(viewModel?.value?.fileProperties?.value?.firstOrNull { it.property == KnownFileProperties.Custom }?.isEditing?.value).isFalse
	}

	@Test
	fun `then the new property is being edited`() {
		assertThat(viewModel?.value?.fileProperties?.value?.firstOrNull { it.property == KnownFileProperties.AlbumArtist }?.isEditing?.value).isTrue
	}

	@Test
	fun `then the new property is highlighted`() {
		assertThat(viewModel?.value?.highlightedProperty?.value).isEqualTo(viewModel?.value?.fileProperties?.value?.firstOrNull { it.property == KnownFileProperties.AlbumArtist })
	}

	@Test
	fun `then the property has the correct editable type`() {
		assertThat(viewModel?.value?.fileProperties?.value?.firstOrNull { it.property == KnownFileProperties.AlbumArtist }?.editableType).isEqualTo(FilePropertyType.ShortFormText)
	}

	@Test
	fun `then the property is edited`() {
		assertThat(viewModel?.value?.fileProperties?.value?.firstOrNull { it.property == KnownFileProperties.AlbumArtist }?.uncommittedValue?.value).isEqualTo("silk")
	}
}
