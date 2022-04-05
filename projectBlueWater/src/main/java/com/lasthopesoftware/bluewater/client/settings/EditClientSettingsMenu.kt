package com.lasthopesoftware.bluewater.client.settings

import android.app.Activity
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.about.AboutActivity
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.views.RemoveLibraryConfirmationDialogBuilder
import com.lasthopesoftware.resources.strings.GetStringResources

class EditClientSettingsMenu(private val activity: Activity, private val stringResources: GetStringResources, private val removeLibraryConfirmationDialog: RemoveLibraryConfirmationDialogBuilder) {
	fun buildSettingsMenu(menu: Menu): Boolean {
		activity.menuInflater.inflate(R.menu.menu_client_settings, menu)
		val menuItem = menu.findItem(R.id.menuAboutApp)
		menuItem.title = stringResources.aboutTitle
		return true
	}

	fun handleSettingsMenuClicks(item: MenuItem, library: Library?): Boolean =
		when(item.itemId) {
			R.id.menuAboutApp -> {
				activity.startActivity(Intent(activity, AboutActivity::class.java))
				true
			}
			R.id.menuRemoveServer -> {
				if (library != null)
					removeLibraryConfirmationDialog.buildRemoveLibraryDialog(library).show()
				true
			}
			else -> false
		}
}
