package com.lasthopesoftware.bluewater.client.settings

import android.content.Intent
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId

interface IEditClientSettingsActivityIntentBuilder {
	fun buildIntent(libraryId: LibraryId): Intent
}
