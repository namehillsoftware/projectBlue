package com.lasthopesoftware.bluewater.client.browsing.items.media.image

import android.graphics.Bitmap
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.namehillsoftware.handoff.promises.Promise

interface ProvideImages {
	fun promiseFileBitmap(serviceFile: ServiceFile): Promise<Bitmap?>
}
