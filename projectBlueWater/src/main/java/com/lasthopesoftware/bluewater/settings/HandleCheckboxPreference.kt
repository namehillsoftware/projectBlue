package com.lasthopesoftware.bluewater.settings

import android.content.SharedPreferences
import android.widget.CheckBox
import android.widget.CompoundButton

internal class HandleCheckboxPreference private constructor(private val sharedPreferences: SharedPreferences, private val settingKey: String) : CompoundButton.OnCheckedChangeListener {
	override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
		sharedPreferences
			.edit()
			.putBoolean(settingKey, isChecked)
			.apply()
	}

	companion object {
		fun handle(sharedPreferences: SharedPreferences, settingKey: String, settingCheckbox: CheckBox) {
			settingCheckbox.isEnabled = false
			val preference = sharedPreferences.getBoolean(settingKey, false)
			settingCheckbox.isChecked = preference
			settingCheckbox.setOnCheckedChangeListener(
				HandleCheckboxPreference(sharedPreferences, settingKey))
			settingCheckbox.isEnabled = true
		}
	}
}
