package com.lasthopesoftware.bluewater.client.browsing.files.properties.GivenAFileProperty.AndAFilePropertyLookup

import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertiesLookup
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FileProperty
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertyType
import com.lasthopesoftware.bluewater.client.browsing.files.properties.NormalizedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ReadOnlyFileProperty
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When the file property is updated` {
	companion object {
		private const val fileProperty = NormalizedFileProperties.LastPlayed
	}

	private val mut by lazy {
		val properties = mutableMapOf(
			fileProperty to "2018-05-06 05:38:29"
		)

		val lookup = object : FilePropertiesLookup() {
			override fun updateValue(name: String, value: String) {
				properties[name] = value
			}

			override val availableProperties: Set<String>
				get() = properties.keys

			override fun getValue(name: String): String? = properties.getOrDefault(name, "")

			override fun isEditable(name: String): Boolean = false
		}

		Pair(properties, lookup)
	}

	private var initialValue: FileProperty? = null
	private var updatedValue: FileProperty? = null

	@BeforeAll
	fun act() {
		val (_, lookup) = mut
		initialValue = lookup.get(fileProperty)
		lookup.update(fileProperty, "1973-12-13 18:08:13")
		updatedValue = lookup.get(fileProperty)
	}

	@Test
	fun `then the initial value is correct`() {
		assertThat(initialValue).isEqualTo(
			ReadOnlyFileProperty(fileProperty,"2018-05-06 05:38:29", FilePropertyType.Date)
		)
	}

	@Test
	fun `then the updated value is correct`() {
		assertThat(updatedValue).isEqualTo(
			ReadOnlyFileProperty(fileProperty,"1973-12-13 18:08:13", FilePropertyType.Date)
		)
	}

	@Test
	fun `then the stored properties are correct`() {
		assertThat(mut.first).isEqualTo(mapOf(
			fileProperty to "1973-12-13 18:08:13"
		))
	}
}
