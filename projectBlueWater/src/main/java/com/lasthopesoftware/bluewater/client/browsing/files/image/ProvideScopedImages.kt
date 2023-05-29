package com.lasthopesoftware.bluewater.client.browsing.files.image

import android.graphics.Bitmap
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.namehillsoftware.handoff.promises.Promise

interface ProvideScopedImages {
	fun promiseFileBitmap(serviceFile: ServiceFile): Promise<Bitmap?>
}
