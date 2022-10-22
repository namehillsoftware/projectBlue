package com.lasthopesoftware.bluewater.client.browsing.files.properties

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.shared.promises.extensions.CancellableProxyPromise
import com.namehillsoftware.handoff.promises.Promise

private fun buildProperties(rawProperties: Map<String, String>): Sequence<FileProperty> = sequence {
	val returnedProperties = HashSet<String>()

	for ((name, value) in rawProperties) {
		if (returnedProperties.add(name))
			yield(FileProperty(name, value))
	}

	for (editable in EditableFilePropertyDefinition.values()) {
		val name = editable.propertyName
		if (!returnedProperties.add(name)) continue

		val value = when(editable.type) {
			FilePropertyType.Integer -> "0"
			else -> ""
		}

		yield(FileProperty(name, value))
	}
}

class EditableScopedFilePropertiesProvider(private val inner:  ProvideScopedFileProperties)
	: ProvideEditableScopedFileProperties {

	override fun promiseFileProperties(serviceFile: ServiceFile): Promise<Sequence<FileProperty>> =
		CancellableProxyPromise { cp ->
			inner
				.promiseFileProperties(serviceFile)
				.apply(cp::doCancel)
				.then(::buildProperties)
		}
}
