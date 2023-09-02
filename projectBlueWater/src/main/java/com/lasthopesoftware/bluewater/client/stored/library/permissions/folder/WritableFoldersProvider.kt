package com.lasthopesoftware.bluewater.client.stored.library.permissions.folder

import android.content.ContentResolver
import android.content.Intent
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.lasthopesoftware.bluewater.shared.promises.extensions.LaunchActivitiesForResults
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import java.net.URI

class WritableFoldersProvider(private val activitiesForResults: LaunchActivitiesForResults, private val contentResolver: ContentResolver) : RequestWritableFolders {
	override fun promiseWritableFolder(): Promise<URI?> = activitiesForResults
		.promiseResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE))
		.then {  result ->
			result.data?.data?.also {
				val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
				// Check for the freshest data.
				contentResolver.takePersistableUriPermission(it, takeFlags)
			}
		}
		.eventually { uri ->
			val documentTreeId = DocumentsContract.getTreeDocumentId(uri)
			val documentUri = DocumentsContract.buildDocumentUriUsingTree(uri, documentTreeId)
			documentUri
				?.let {
					QueuedPromise(MessageWriter {
						contentResolver
							.query(it, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
							?.use { c ->
								c.takeIf { it.moveToFirst() }?.run {
									val path = getString(0)
									URI(path)
								}
							}
					}, ThreadPools.io)
				}
				.keepPromise()
		}
}
