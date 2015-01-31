package com.lasthopesoftware.bluewater.servers.library.items.files.playback.service.receivers;

import com.lasthopesoftware.bluewater.servers.library.items.files.playback.service.PlaybackService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AudioBecomingNoisyReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		PlaybackService.pause(context);
	}
}
