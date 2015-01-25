package com.lasthopesoftware.bluewater.servers.library.items.files.nowplaying.service.receivers;

import com.lasthopesoftware.bluewater.servers.library.items.files.nowplaying.service.NowPlayingService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AudioBecomingNoisyReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		NowPlayingService.pause(context);
	}
}
