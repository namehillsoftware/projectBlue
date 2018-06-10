package com.lasthopesoftware.bluewater.client.playback.service.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService;

import static android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY;

public class AudioBecomingNoisyReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if (ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction()))
			PlaybackService.pause(context);
	}
}
