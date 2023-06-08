package com.lasthopesoftware.bluewater.client.browsing.files.broadcasts

import java.io.File

/**
 * Created by david on 7/3/16.
 */
interface IScanMediaFileBroadcaster {
    fun sendScanMediaFileBroadcastForFile(file: File?)
}
