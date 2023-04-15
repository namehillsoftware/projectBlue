package com.lasthopesoftware.bluewater.client.connection.polling

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage

class ConnectionLostNotification(val libraryId: LibraryId) : ApplicationMessage
