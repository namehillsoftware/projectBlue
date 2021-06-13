package com.lasthopesoftware.bluewater.client.connection.builder

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId

class MissingConnectionSettingsException(val libraryId: LibraryId)
	: Exception("Connection settings were not found for $libraryId")
