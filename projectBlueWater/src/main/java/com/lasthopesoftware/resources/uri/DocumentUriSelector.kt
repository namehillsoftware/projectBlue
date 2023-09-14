package com.lasthopesoftware.resources.uri

import android.content.Intent
import android.provider.DocumentsContract
import com.lasthopesoftware.bluewater.shared.promises.extensions.LaunchActivitiesForResults
import com.namehillsoftware.handoff.promises.Promise
import java.net.URI

class DocumentUriSelector(private val activitiesForResults: LaunchActivitiesForResults) : SelectDocumentUris {
	override fun promiseSelectedDocumentUri(vararg mimeTypes: String): Promise<URI?> = activitiesForResults
		.promiseResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
			type = "*/*"

			if (mimeTypes.size < 2) type = mimeTypes.firstOrNull()
			else putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
		})
		.then { result ->
			result.data?.data?.let { uri ->
				val documentId = DocumentsContract.getDocumentId(uri)
				DocumentsContract.buildDocumentUri(uri.authority, documentId).toURI()
			}
		}

}
