package com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.PlaybackService;

public class AudioBecomingNoisyReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		PlaybackService.pause(context);
	}
}
