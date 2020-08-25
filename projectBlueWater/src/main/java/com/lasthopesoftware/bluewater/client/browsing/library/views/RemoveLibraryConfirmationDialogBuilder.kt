package com.lasthopesoftware.bluewater.client.browsing.library.views

import android.app.AlertDialog
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.library.access.RemoveLibraries
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.settings.EditClientSettingsActivity

class RemoveLibraryConfirmationDialogBuilder(private val activity: EditClientSettingsActivity, private val libraryRemoval: RemoveLibraries) {

	fun buildRemoveLibraryDialog(library: Library): AlertDialog.Builder {
		val message = String.format(activity.getString(R.string.confirmServerRemoval), library.accessCode)
		val builder = AlertDialog.Builder(activity, R.style.DialogTheme)
		builder.setTitle(activity.getText(R.string.removeServer)).setMessage(message).setCancelable(true)

		builder.setPositiveButton(R.string.yes) { _, _ -> libraryRemoval.removeLibrary(library).then { activity.finish() } }
		builder.setNegativeButton(R.string.no) { dialog, _ -> dialog.dismiss() }

		return builder
	}
}
