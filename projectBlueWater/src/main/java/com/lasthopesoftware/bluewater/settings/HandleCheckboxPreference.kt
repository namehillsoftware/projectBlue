package com.lasthopesoftware.bluewater.settings

import android.widget.CheckBox
import android.widget.CompoundButton
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettings
import com.lasthopesoftware.bluewater.settings.repository.access.HoldApplicationSettings

internal class HandleCheckboxPreference private constructor(private val applicationSettings: HoldApplicationSettings, private val updateSetting: (ApplicationSettings) -> (Boolean) -> Unit) : CompoundButton.OnCheckedChangeListener {
	override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
		applicationSettings.promiseApplicationSettings()
			.then { s ->
				updateSetting(s)(isChecked)
				applicationSettings.promiseUpdatedSettings(s)
			}
	}

	companion object {
		fun handle(applicationSettings: HoldApplicationSettings, getSetting: (ApplicationSettings) -> Boolean, updateSetting: (ApplicationSettings) -> (Boolean) -> Unit, settingCheckbox: CheckBox) {
			settingCheckbox.isEnabled = false
			applicationSettings.promiseApplicationSettings()
				.then { s ->
					val preference = getSetting(s)
					settingCheckbox.isChecked = preference
					settingCheckbox.setOnCheckedChangeListener(
						HandleCheckboxPreference(applicationSettings, updateSetting))
					settingCheckbox.isEnabled = true
				}
		}
	}
}
