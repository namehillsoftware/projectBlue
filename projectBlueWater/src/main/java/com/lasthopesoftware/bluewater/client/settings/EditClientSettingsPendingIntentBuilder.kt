package com.lasthopesoftware.bluewater.client.settings

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.android.makePendingIntentImmutable

class EditClientSettingsPendingIntentBuilder(
    private val context: Context,
    private val editClientSettingsActivityIntentBuilder: IEditClientSettingsActivityIntentBuilder = EditClientSettingsActivityIntentBuilder(context)
) : IEditClientSettingsPendingIntentBuilder {
    override fun buildEditServerSettingsPendingIntent(libraryId: Int): PendingIntent {
        val settingsIntent = editClientSettingsActivityIntentBuilder.buildIntent(LibraryId(libraryId))
        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(context, 0, settingsIntent, 0.makePendingIntentImmutable())
    }
}
