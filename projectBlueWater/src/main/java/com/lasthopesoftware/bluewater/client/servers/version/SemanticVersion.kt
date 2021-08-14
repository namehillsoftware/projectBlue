package com.lasthopesoftware.bluewater.client.servers.version

data class SemanticVersion(val major: Int, val minor: Int, val patch: Int, val label: String? = "") {
	override fun toString(): String = "$major.$minor.$patch$label"
}
