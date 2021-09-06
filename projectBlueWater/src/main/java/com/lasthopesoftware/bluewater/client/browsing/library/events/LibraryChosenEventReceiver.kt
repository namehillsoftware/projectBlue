package com.lasthopesoftware.bluewater.client.browsing.library.events

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lasthopesoftware.bluewater.client.browsing.BrowserEntryActivity
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.BrowserLibrarySelection

class LibraryChosenEventReceiver(private val browserEntryActivity: BrowserEntryActivity) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val chosenLibrary = intent.getIntExtra(BrowserLibrarySelection.chosenLibraryId, -1)
        if (chosenLibrary >= 0) browserEntryActivity.finishAffinity()
    }
}
