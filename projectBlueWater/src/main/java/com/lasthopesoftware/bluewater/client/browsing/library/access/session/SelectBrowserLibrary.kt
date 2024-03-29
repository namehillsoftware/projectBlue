package com.lasthopesoftware.bluewater.client.browsing.library.access.session

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface SelectBrowserLibrary {
    fun selectBrowserLibrary(libraryId: LibraryId): Promise<Library>
}
