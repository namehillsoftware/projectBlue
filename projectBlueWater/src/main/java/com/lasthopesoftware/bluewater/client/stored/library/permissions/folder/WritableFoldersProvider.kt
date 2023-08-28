package com.lasthopesoftware.bluewater.client.stored.library.permissions.folder

import android.content.ContentResolver
import android.content.Intent
import com.lasthopesoftware.bluewater.shared.promises.extensions.LaunchActivitiesForResults
import com.lasthopesoftware.resources.uri.toURI
import com.namehillsoftware.handoff.promises.Promise
import java.net.URI

class WritableFoldersProvider(private val activitiesForResults: LaunchActivitiesForResults, private val contentResolver: ContentResolver) : RequestWritableFolders {
	override fun promiseWritableFolder(): Promise<URI?> = activitiesForResults
		.promiseResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE))
		.then {  result ->
			result.data?.data?.also {
				val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
				// Check for the freshest data.
				contentResolver.takePersistableUriPermission(it, takeFlags)
			}?.toURI()
		}
}
