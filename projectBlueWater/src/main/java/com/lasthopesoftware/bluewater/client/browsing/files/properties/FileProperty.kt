package com.lasthopesoftware.bluewater.client.browsing.files.properties

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FileProperty(val name: String, val value: String) : Parcelable

val FileProperty.editableFilePropertyDefinition: EditableFilePropertyDefinition?
	get() = EditableFilePropertyDefinition.fromName(name)
