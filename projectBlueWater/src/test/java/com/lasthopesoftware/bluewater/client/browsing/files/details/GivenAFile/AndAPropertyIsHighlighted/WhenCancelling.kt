package com.lasthopesoftware.bluewater.client.browsing.files.details.GivenAFile.AndAPropertyIsHighlighted

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertiesLookup
import com.lasthopesoftware.bluewater.client.browsing.files.properties.NormalizedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.url.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.URL

class WhenHighlightingTheProperty {
	companion object {
		private const val libraryId = 72
		private const val serviceFileId = "300"
	}

	private val viewModel by lazy {
		FileDetailsViewModel(
			mockk {
				every { promiseIsReadOnly(LibraryId(libraryId)) } returns false.toPromise()
			},
			mockk {
				every { promiseFileProperties(LibraryId(libraryId), ServiceFile(serviceFileId)) } returns Promise(
					object : FilePropertiesLookup() {
						override val availableProperties: Set<String>
							get() = setOf("bread", "silence")

						override fun getValue(name: String): String? = when (name) {
							NormalizedFileProperties.Rating -> "412"
							"simple" -> "middle"
							"aside" -> "vessel"
							NormalizedFileProperties.Name -> "skin"
							NormalizedFileProperties.Artist -> "afford"
							NormalizedFileProperties.Genre -> "avenue"
							NormalizedFileProperties.Lyrics -> "regret"
							NormalizedFileProperties.Comment -> "dream"
							NormalizedFileProperties.Composer -> "risk"
							NormalizedFileProperties.Custom -> "fate"
							NormalizedFileProperties.Publisher -> "crash"
							NormalizedFileProperties.TotalDiscs -> "bone"
							NormalizedFileProperties.Track -> "passage"
							NormalizedFileProperties.AlbumArtist -> "enclose"
							NormalizedFileProperties.Album -> "amuse"
							NormalizedFileProperties.Date -> "9357"
							else -> null
						}

						override fun isEditable(name: String): Boolean = false

						override fun update(name: String, value: String) {}
					}
				)
			},
			mockk(),
			mockk {
				every { promiseImageBytes() } returns byteArrayOf(3, 4).toPromise()
			},
			mockk {
				every { promiseImageBytes(LibraryId(libraryId), any<ServiceFile>()) } returns byteArrayOf(
					61,
					127
				).toPromise()
			},
			mockk(),
			RecordingApplicationMessageBus(),
			mockk {
				every {
					promiseUrlKey(
						LibraryId(libraryId),
						ServiceFile(serviceFileId)
					)
				} returns UrlKeyHolder(URL("http://bow"), ServiceFile(serviceFileId)).toPromise()
			},
		)
	}

	@BeforeAll
	fun act() {
		viewModel.apply {
			load(LibraryId(libraryId), ServiceFile(serviceFileId)).toExpiringFuture().get()
			fileProperties.value.first { it.propertyName == NormalizedFileProperties.Date }.apply {
				highlight()
				cancel()
			}
		}
	}

	@Test
	fun `then there is no highlighted property`() {
		assertThat(viewModel.highlightedProperty.value).isNull()
	}
}
