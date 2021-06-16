package com.lasthopesoftware.bluewater.client.connection.settings

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId

class MissingAccessCodeException(libraryId: LibraryId) : Exception("The access code was not available for library $libraryId")
