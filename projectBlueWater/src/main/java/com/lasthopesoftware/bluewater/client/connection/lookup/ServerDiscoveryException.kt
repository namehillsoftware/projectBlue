package com.lasthopesoftware.bluewater.client.connection.lookup

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId

class ServerDiscoveryException : Exception {
    internal constructor(
        library: LibraryId,
        serverMessage: String
    ) : super("Unable to find server for library ${library.id}, the server responded with: \"$serverMessage\".") {
    }

    internal constructor(library: LibraryId) : super("Unable to find server for library ${library.id}.")
}
