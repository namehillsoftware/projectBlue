package com.lasthopesoftware.bluewater.client.browsing.files.properties.EditableFileProperties.GivenFileProperties.AndSomeEditableOnesAreNotPresent

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.*
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val serviceFileId = 907

class WhenGettingTheFileProperties {
	private val editableFilePropertyProvider by lazy {
		EditableScopedFilePropertiesProvider(
			mockk {
				every { promiseFileProperties(ServiceFile(serviceFileId)) } returns Promise(
					mapOf(
						Pair(KnownFileProperties.DateFirstRated, "Nr13052"),
						Pair(KnownFileProperties.AlbumArtist, "MB4Q"),
						Pair("Aj8", "4vBz"),
					)
				)
			}
		)
	}

	private lateinit var fileProperties: Sequence<FileProperty>

	@BeforeAll
	fun act() {
		fileProperties = editableFilePropertyProvider.promiseFileProperties(ServiceFile(serviceFileId)).toExpiringFuture().get() ?: emptySequence()
	}

	@Test
	fun `then the returned file properties are correct`() {
		assertThat(fileProperties.toList()).hasSameElementsAs(
			listOf(
				FileProperty(KnownFileProperties.DateFirstRated, "Nr13052"),
				FileProperty(KnownFileProperties.AlbumArtist, "MB4Q"),
				FileProperty("Aj8", "4vBz"),
			).plus(EditableFilePropertyDefinition.values().filterNot { arrayOf(KnownFileProperties.DateFirstRated, KnownFileProperties.AlbumArtist, "Aj8").contains(it.propertyName) }.map {
				when(it.type) {
					FilePropertyType.Integer -> FileProperty(it.propertyName, "0")
					else -> FileProperty(it.propertyName, "")
				}
			})
		)
	}
}
