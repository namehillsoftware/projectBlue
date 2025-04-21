package com.lasthopesoftware.bluewater.client.browsing.files.properties

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

class EditableLibraryFilePropertiesProvider(
	private val inner: ProvideFreshLibraryFileProperties,
	private val editableDefinitions: ProvideEditableFilePropertyDefinitions,
): ProvideEditableLibraryFileProperties {

	private fun buildProperties(editableDefinitions: Set<EditableFilePropertyDefinition>, rawProperties: Map<String, String>): Sequence<FileProperty> = sequence {
		fun getFileProperty(name: String, value: String): FileProperty {
			val definition = EditableFilePropertyDefinition.fromName(name)
			return if (definition != null && editableDefinitions.contains(definition)) EditableFileProperty(name, value)
			else ReadOnlyFileProperty(name, value)
		}

		val returnedProperties = HashSet<String>()

		for ((name, value) in rawProperties) {
			if (returnedProperties.add(name))
				yield(getFileProperty(name, value))
		}

		for (editable in EditableFilePropertyDefinition.entries) {
			val name = editable.propertyName
			if (!returnedProperties.add(name)) continue

			val value = when(editable.type) {
				FilePropertyType.Integer -> "0"
				else -> ""
			}

			yield(getFileProperty(name, value))
		}
	}

	override fun promiseFileProperties(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Sequence<FileProperty>> =
		Promise.Proxy { cp ->
			inner
				.promiseFileProperties(libraryId, serviceFile)
				.apply(cp::doCancel)
				.eventually { properties ->
					editableDefinitions
						.promiseEditableFilePropertyDefinitions(libraryId)
						.also(cp::doCancel)
						.then { definitions ->
							buildProperties(definitions, properties)
						}
				}
		}
}
