package com.lasthopesoftware.bluewater.client.browsing.files.properties

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

interface FileProperty : Parcelable {
	val name: String
	val value: String
}

@Parcelize
data class ReadOnlyFileProperty(override val name: String, override val value: String) : FileProperty, Parcelable

@Parcelize
data class EditableFileProperty(override val name: String, override val value: String) : FileProperty, Parcelable

val FileProperty.editableFilePropertyDefinition: EditableFilePropertyDefinition?
	get() = EditableFilePropertyDefinition.fromName(name)
