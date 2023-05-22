package com.lasthopesoftware.bluewater.client.browsing.files.uri

import android.net.Uri
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

fun interface ProvideFileUris {
    fun promiseFileUri(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Uri?>
}
