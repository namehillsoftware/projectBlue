package com.lasthopesoftware.bluewater.client.browsing.items.media.files.uri

import android.net.Uri
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.namehillsoftware.handoff.promises.Promise

fun interface IFileUriProvider {
    fun promiseFileUri(serviceFile: ServiceFile): Promise<Uri?>
}
