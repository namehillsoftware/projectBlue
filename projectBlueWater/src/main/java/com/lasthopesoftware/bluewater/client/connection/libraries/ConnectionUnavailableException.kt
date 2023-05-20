package com.lasthopesoftware.bluewater.client.connection.libraries

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import java.io.IOException

class ConnectionUnavailableException(libraryId: LibraryId) : IOException("A connection was not returned for Library with ID: $libraryId.")
