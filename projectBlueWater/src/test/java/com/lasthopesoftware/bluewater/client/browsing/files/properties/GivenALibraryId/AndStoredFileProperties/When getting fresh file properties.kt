package com.lasthopesoftware.bluewater.client.browsing.files.properties.GivenALibraryId.AndStoredFileProperties

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FakeFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FreshestRevisionFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.LookupFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.MappedFilePropertiesLookup
import com.lasthopesoftware.bluewater.client.browsing.files.properties.NormalizedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.PassThroughFilePropertiesLookup
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ReadOnlyFileProperty
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertiesContainer
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.url.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.URL

class `When getting fresh file properties` {

	private val libraryId = LibraryId(649)
	private val serviceFile = ServiceFile("Mx68INXDUIY")
	private val expectedContainerRevision = 792749L
	private val fakeFilePropertiesContainer by lazy {
		FakeFilePropertiesContainerRepository().apply {
			putFilePropertiesContainer(
				UrlKeyHolder(URL("http://example.com"), serviceFile),
				FilePropertiesContainer(692, MappedFilePropertiesLookup(mapOf(Pair("package", "heighten"))))
			)
		}
	}

	private val mut by lazy {
		FreshestRevisionFilePropertiesProvider(
			mockk {
				every { promiseFileProperties(libraryId, serviceFile) } returns PassThroughFilePropertiesLookup(
					listOf(
						ReadOnlyFileProperty(NormalizedFileProperties.Rating, "3"),
						ReadOnlyFileProperty("Rhoncusmollis", "Risusquisque"),
						ReadOnlyFileProperty(NormalizedFileProperties.Name, "Tristiquelectus"),
						ReadOnlyFileProperty(NormalizedFileProperties.Artist, "Pellentesquesed"),
						ReadOnlyFileProperty(NormalizedFileProperties.Album, "Lectusnon"),
						ReadOnlyFileProperty(NormalizedFileProperties.Track, "657"),
					)
				).toPromise()
			},
			mockk {
				every { promiseUrlKey(libraryId, serviceFile) } returns UrlKeyHolder(URL("http://example.com"), serviceFile).toPromise()
			},
			mockk {
				every { promiseRevision(libraryId) } returns expectedContainerRevision.toPromise()
			},
			fakeFilePropertiesContainer
		)
	}

	private var fileProperties: LookupFileProperties? = null

	@BeforeAll
	fun act() {
		fileProperties = mut.promiseFileProperties(libraryId, serviceFile).toExpiringFuture().get()
	}

	@Test
	fun `then the returned file properties are correct`() {
		assertThat(fileProperties?.allProperties?.toList()).isEqualTo(
			listOf(
				ReadOnlyFileProperty(NormalizedFileProperties.Rating, "3"),
				ReadOnlyFileProperty("Rhoncusmollis", "Risusquisque"),
				ReadOnlyFileProperty(NormalizedFileProperties.Name, "Tristiquelectus"),
				ReadOnlyFileProperty(NormalizedFileProperties.Artist, "Pellentesquesed"),
				ReadOnlyFileProperty(NormalizedFileProperties.Album, "Lectusnon"),
				ReadOnlyFileProperty(NormalizedFileProperties.Track, "657"),
			)
		)
	}

	@Test
	fun `then the contained file properties are updated`() {
		assertThat(
			fakeFilePropertiesContainer.storage[UrlKeyHolder(
				URL("http://example.com"),
				serviceFile
			)]?.properties?.allProperties?.toList()
		).isEqualTo(
			listOf(
				ReadOnlyFileProperty(NormalizedFileProperties.Rating, "3"),
				ReadOnlyFileProperty("Rhoncusmollis", "Risusquisque"),
				ReadOnlyFileProperty(NormalizedFileProperties.Name, "Tristiquelectus"),
				ReadOnlyFileProperty(NormalizedFileProperties.Artist, "Pellentesquesed"),
				ReadOnlyFileProperty(NormalizedFileProperties.Album, "Lectusnon"),
				ReadOnlyFileProperty(NormalizedFileProperties.Track, "657"),
			)
		)
	}

	@Test
	fun `then the file properties container revision is updated`() {
		assertThat(
			fakeFilePropertiesContainer.storage[UrlKeyHolder(
				URL("http://example.com"),
				serviceFile
			)]?.revision
		).isEqualTo(expectedContainerRevision)
	}
}
