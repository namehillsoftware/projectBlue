package com.lasthopesoftware.bluewater.client.stored.library.items

import com.lasthopesoftware.bluewater.client.browsing.items.KeyedIdentifier
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage

class SyncItemStateChanged(val libraryId: LibraryId, val itemId: KeyedIdentifier, val isSynced: Boolean) : ApplicationMessage
