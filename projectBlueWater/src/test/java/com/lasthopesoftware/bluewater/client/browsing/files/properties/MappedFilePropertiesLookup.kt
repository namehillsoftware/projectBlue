package com.lasthopesoftware.bluewater.client.browsing.files.properties

open class MappedFilePropertiesLookup(filePropertiesMap: Map<String, String> = mutableMapOf()) : FilePropertiesLookup() {

	private val filePropertiesMap = filePropertiesMap.toMutableMap()

	override val availableProperties: Set<String>
		get() = filePropertiesMap.keys

	override fun getValue(name: String): String? = filePropertiesMap[name]

	override fun isEditable(name: String): Boolean = false

	override fun updateValue(name: String, value: String) {
		filePropertiesMap[name] = value
	}
}
