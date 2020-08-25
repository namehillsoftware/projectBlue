package com.lasthopesoftware.bluewater.client.settings

import android.content.Intent

interface IEditClientSettingsActivityIntentBuilder {
	fun buildIntent(libraryId: Int): Intent
}
