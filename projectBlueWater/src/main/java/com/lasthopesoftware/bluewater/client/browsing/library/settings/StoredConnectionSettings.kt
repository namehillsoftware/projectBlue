package com.lasthopesoftware.bluewater.client.browsing.library.settings

sealed interface StoredConnectionSettings {
	val customHeaders: Map<String, String>
}
