package com.lasthopesoftware.bluewater.settings

import android.widget.CheckBox
import android.widget.CompoundButton
import com.lasthopesoftware.bluewater.client.stored.sync.ScheduleSyncs
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettings
import com.lasthopesoftware.bluewater.settings.repository.access.HoldApplicationSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise

internal class HandleSyncCheckboxPreference private constructor(
	private val applicationSettings: HoldApplicationSettings,
	private val syncScheduler: ScheduleSyncs,
	private val updateSetting: (ApplicationSettings) -> (Boolean) -> Unit
) : CompoundButton.OnCheckedChangeListener {
	override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
		applicationSettings.promiseApplicationSettings()
			.eventually { s ->
				updateSetting(s)(isChecked)
				applicationSettings.promiseUpdatedSettings(s)
			}
			.eventually(LoopedInPromise.response({ syncScheduler.scheduleSync() }, buttonView.context))
	}

	companion object {
		fun handle(applicationSettings: HoldApplicationSettings, syncScheduler: ScheduleSyncs, getSetting: (ApplicationSettings) -> Boolean, updateSetting: (ApplicationSettings) -> (Boolean) -> Unit, settingCheckbox: CheckBox) {
			settingCheckbox.isEnabled = false
			applicationSettings.promiseApplicationSettings()
				.eventually(LoopedInPromise.response({ s ->
					val preference = getSetting(s)
					settingCheckbox.isChecked = preference
					settingCheckbox.setOnCheckedChangeListener(
						HandleSyncCheckboxPreference(applicationSettings, syncScheduler, updateSetting))
					settingCheckbox.isEnabled = true
				}, settingCheckbox.context))
		}
	}
}
