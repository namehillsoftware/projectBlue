package com.lasthopesoftware.bluewater.client.servers.list.listeners

import android.app.Activity
import android.view.View
import com.lasthopesoftware.bluewater.client.settings.EditClientSettingsActivityIntentBuilder
import com.lasthopesoftware.bluewater.client.settings.IEditClientSettingsActivityIntentBuilder

class EditServerClickListener(private val activity: Activity, private val editClientSettingsActivityIntentBuilder: IEditClientSettingsActivityIntentBuilder, private val libraryId: Int) : View.OnClickListener {
	constructor(activity: Activity, libraryId: Int) : this(activity, EditClientSettingsActivityIntentBuilder(activity), libraryId)

	override fun onClick(v: View) =
		activity.startActivityForResult(editClientSettingsActivityIntentBuilder.buildIntent(libraryId), 5388)
}
