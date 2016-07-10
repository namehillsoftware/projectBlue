package com.lasthopesoftware.bluewater.client.library.items.media.files.broadcasts;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.File;

/**
 * Created by david on 7/3/16.
 */
public class ScanMediaFileBroadcaster implements IScanMediaFileBroadcaster {
	private final Context context;

	public ScanMediaFileBroadcaster(Context context) {
		this.context = context;
	}

	@Override
	public void sendScanMediaFileBroadcastForFile(File file) {
		context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
	}
}
