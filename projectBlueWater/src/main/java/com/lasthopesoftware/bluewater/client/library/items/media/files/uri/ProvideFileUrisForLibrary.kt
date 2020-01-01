package com.lasthopesoftware.bluewater.client.library.items.media.files.uri

import android.net.Uri
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface ProvideFileUrisForLibrary {
	fun promiseUri(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Uri>
}
