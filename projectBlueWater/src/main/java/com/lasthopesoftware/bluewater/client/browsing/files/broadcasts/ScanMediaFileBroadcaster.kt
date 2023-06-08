package com.lasthopesoftware.bluewater.client.browsing.files.broadcasts

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.io.File

/**
 * Created by david on 7/3/16.
 */
class ScanMediaFileBroadcaster(private val context: Context) : IScanMediaFileBroadcaster {
    override fun sendScanMediaFileBroadcastForFile(file: File?) {
        context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))
    }
}
