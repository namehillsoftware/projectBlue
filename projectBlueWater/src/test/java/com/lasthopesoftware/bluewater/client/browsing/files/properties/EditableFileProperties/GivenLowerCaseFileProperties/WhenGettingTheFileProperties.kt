package com.lasthopesoftware.bluewater.client.browsing.files.properties.EditableFileProperties.GivenLowerCaseFileProperties

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.EditableFileProperty
import com.lasthopesoftware.bluewater.client.browsing.files.properties.EditableFilePropertyDefinition
import com.lasthopesoftware.bluewater.client.browsing.files.properties.EditableLibraryFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FileProperty
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertyType
import com.lasthopesoftware.bluewater.client.browsing.files.properties.NormalizedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ReadOnlyFileProperty
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
		private const val libraryId = 926
		private const val serviceFileId = "517.5"
	}

	private val editableFilePropertyProvider by lazy {
		EditableLibraryFilePropertiesProvider(
			mockk {
				every { promiseFileProperties(LibraryId(libraryId), ServiceFile(serviceFileId)) } returns Promise(
					mapOf(
						Pair(NormalizedFileProperties.Rating.lowercase(), "218"),
						Pair("Natoquefacilisis", "Semperlobortis"),
						Pair("Vivamusdapibus", "Placeratinceptos"),
						Pair(NormalizedFileProperties.Name.lowercase(), "Vulputateornare"),
						Pair(NormalizedFileProperties.Artist.lowercase(), "Consequattempor"),
						Pair(NormalizedFileProperties.Album.lowercase(), "Dignissimorci"),
						Pair(NormalizedFileProperties.Composer.lowercase(), "Facilisimalesuada"),
						Pair(NormalizedFileProperties.AlbumArtist.lowercase(), "Condimentummaecenas"),
					)
				)
			},
			mockk {
				every { promiseEditableFilePropertyDefinitions(LibraryId(libraryId)) } returns Promise(
					setOf(
						EditableFilePropertyDefinition.Artist,
						EditableFilePropertyDefinition.Rating,
						EditableFilePropertyDefinition.DiscNumber
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
		val expectedLoadedProperties = listOf(
			EditableFileProperty(NormalizedFileProperties.Rating, "218"),
			ReadOnlyFileProperty("Natoquefacilisis", "Semperlobortis"),
			ReadOnlyFileProperty("Vivamusdapibus", "Placeratinceptos"),
			ReadOnlyFileProperty(NormalizedFileProperties.Name, "Vulputateornare"),
			EditableFileProperty(NormalizedFileProperties.Artist, "Consequattempor"),
			ReadOnlyFileProperty(NormalizedFileProperties.Album, "Dignissimorci"),
			ReadOnlyFileProperty(NormalizedFileProperties.Composer, "Facilisimalesuada"),
			ReadOnlyFileProperty(NormalizedFileProperties.AlbumArtist, "Condimentummaecenas"),
		)
		assertThat(fileProperties.toList()).hasSameElementsAs(
			expectedLoadedProperties + (EditableFilePropertyDefinition.entries
				.filterNot { e -> expectedLoadedProperties.any { it.name == e.propertyName } }
				.map {
					when (it) {
						EditableFilePropertyDefinition.DiscNumber -> EditableFileProperty(it.propertyName, "0")
						else -> {
							when(it.type) {
								FilePropertyType.Integer -> ReadOnlyFileProperty(it.propertyName, "0")
								else -> ReadOnlyFileProperty(it.propertyName, "")
							}
						}
					}
				})
		)
	}
}
