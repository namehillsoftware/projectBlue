package com.lasthopesoftware.bluewater.client.browsing.files.properties

data class FileProperty(val name: String, val value: String) {
	val editableFilePropertyDefinition by lazy { EditableFilePropertyDefinition.fromName(name) }
}
