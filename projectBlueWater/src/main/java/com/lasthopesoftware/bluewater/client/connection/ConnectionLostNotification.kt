package com.lasthopesoftware.bluewater.client.connection

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage

data class ConnectionLostNotification(val libraryId: LibraryId) : ApplicationMessage
