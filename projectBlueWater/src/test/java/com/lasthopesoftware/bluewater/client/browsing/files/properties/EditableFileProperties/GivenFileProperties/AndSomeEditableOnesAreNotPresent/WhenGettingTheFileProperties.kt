package com.lasthopesoftware.bluewater.client.browsing.files.properties.EditableFileProperties.GivenFileProperties.AndSomeEditableOnesAreNotPresent

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.EditableFilePropertyDefinition
import com.lasthopesoftware.bluewater.client.browsing.files.properties.EditableLibraryFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FileProperty
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertyType
import com.lasthopesoftware.bluewater.client.browsing.files.properties.NormalizedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenGettingTheFileProperties {
	companion object {
		private const val libraryId = 554
		private const val serviceFileId = "907"
	}

	private val editableFilePropertyProvider by lazy {
		EditableLibraryFilePropertiesProvider(
			mockk {
				every { promiseFileProperties(LibraryId(libraryId), ServiceFile(serviceFileId)) } returns Promise(
					mapOf(
						Pair(NormalizedFileProperties.DateFirstRated, "Nr13052"),
						Pair(NormalizedFileProperties.AlbumArtist, "MB4Q"),
						Pair("Aj8", "4vBz"),
					)
				)
			}
		)
	}

	private lateinit var fileProperties: Sequence<FileProperty>

	@BeforeAll
	fun act() {
		fileProperties = editableFilePropertyProvider.promiseFileProperties(LibraryId(libraryId), ServiceFile(serviceFileId)).toExpiringFuture().get() ?: emptySequence()
	}

	@Test
	fun `then the returned file properties are correct`() {
		assertThat(fileProperties.toList()).hasSameElementsAs(
			listOf(
				FileProperty(NormalizedFileProperties.DateFirstRated, "Nr13052"),
				FileProperty(NormalizedFileProperties.AlbumArtist, "MB4Q"),
				FileProperty("Aj8", "4vBz"),
			).plus(EditableFilePropertyDefinition.entries.filterNot { arrayOf(NormalizedFileProperties.DateFirstRated, NormalizedFileProperties.AlbumArtist, "Aj8").contains(it.propertyName) }.map {
				when(it.type) {
					FilePropertyType.Integer -> FileProperty(it.propertyName, "0")
					else -> FileProperty(it.propertyName, "")
				}
			})
		)
	}
}
