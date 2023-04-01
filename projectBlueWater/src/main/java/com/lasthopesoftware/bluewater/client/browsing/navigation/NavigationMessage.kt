package com.lasthopesoftware.bluewater.client.browsing.navigation

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.messages.TypedMessage

interface NavigationMessage : TypedMessage

class ViewDownloadsMessage(val libraryId: LibraryId): NavigationMessage

class ViewServerSettingsMessage(val libraryId: LibraryId): NavigationMessage

object ViewApplicationSettingsMessage: NavigationMessage
