package com.lasthopesoftware.bluewater.client.browsing.library.access.session

import android.content.Context
import androidx.preference.PreferenceManager
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.settings.repository.ApplicationConstants

/**
 * Created by david on 2/12/17.
 */
class SelectedBrowserLibraryIdentifierProvider(private val context: Context) : ProvideSelectedLibraryId {
	override val selectedLibraryId: LibraryId?
		get() {
			val libraryId = PreferenceManager.getDefaultSharedPreferences(context).getInt(ApplicationConstants.PreferenceConstants.chosenLibraryKey, -1)
			return if (libraryId > -1) LibraryId(libraryId) else null
		}
}
