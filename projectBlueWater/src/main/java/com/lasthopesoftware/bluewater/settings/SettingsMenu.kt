package com.lasthopesoftware.bluewater.settings

import android.app.Activity
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.about.AboutActivity
import com.lasthopesoftware.resources.strings.GetStringResources

class SettingsMenu(private val activity: Activity, private val stringResources: GetStringResources) {
	fun buildSettingsMenu(menu: Menu): Boolean {
		activity.menuInflater.inflate(R.menu.menu_settings, menu)
		val menuItem = menu.findItem(R.id.menu_about_app)
		menuItem.title = stringResources.aboutTitle
		return true
	}

	fun handleSettingsMenuClicks(item: MenuItem): Boolean {
		if (item.itemId != R.id.menu_about_app) return false
		activity.startActivity(Intent(activity, AboutActivity::class.java))
		return true
	}
}
