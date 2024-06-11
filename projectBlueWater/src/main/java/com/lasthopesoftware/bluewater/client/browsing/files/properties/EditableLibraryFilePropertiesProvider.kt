package com.lasthopesoftware.bluewater.client.browsing.files.properties

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

private fun buildProperties(rawProperties: Map<String, String>): Sequence<FileProperty> = sequence {
	val returnedProperties = HashSet<String>()

	for ((name, value) in rawProperties) {
		if (returnedProperties.add(name))
			yield(FileProperty(name, value))
	}

	for (editable in EditableFilePropertyDefinition.entries) {
		val name = editable.propertyName
		if (!returnedProperties.add(name)) continue

		val value = when(editable.type) {
			FilePropertyType.Integer -> "0"
			else -> ""
		}

		yield(FileProperty(name, value))
	}
}

class EditableLibraryFilePropertiesProvider(private val inner: ProvideLibraryFileProperties): ProvideEditableLibraryFileProperties {
	override fun promiseFileProperties(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Sequence<FileProperty>> =
		Promise.Proxy { cp ->
			inner
				.promiseFileProperties(libraryId, serviceFile)
				.apply(cp::doCancel)
				.then(::buildProperties)
		}
}
