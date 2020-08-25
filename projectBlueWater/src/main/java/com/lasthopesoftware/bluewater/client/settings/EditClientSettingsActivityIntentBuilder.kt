package com.lasthopesoftware.bluewater.client.settings

import android.content.Context
import android.content.Intent
import com.lasthopesoftware.bluewater.client.settings.EditClientSettingsActivity
import com.lasthopesoftware.resources.intents.IIntentFactory
import com.lasthopesoftware.resources.intents.IntentFactory

class EditClientSettingsActivityIntentBuilder(private val intentFactory: IIntentFactory) : IEditClientSettingsActivityIntentBuilder {
	constructor(context: Context) : this(IntentFactory(context))

	override fun buildIntent(libraryId: Int): Intent {
		val returnIntent = intentFactory.getIntent(EditClientSettingsActivity::class.java)
		returnIntent.putExtra(EditClientSettingsActivity.serverIdExtra, libraryId)
		return returnIntent
	}
}
