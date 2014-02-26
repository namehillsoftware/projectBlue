package com.lasthopesoftware.bluewater.receivers;

import com.lasthopesoftware.bluewater.services.StreamingMusicService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

public class RemoteControlReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		final KeyEvent event = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
	    if (event.getAction() != KeyEvent.ACTION_DOWN) return;
	    
	    switch (event.getKeyCode()) {
	        case KeyEvent.KEYCODE_MEDIA_STOP:
	            StreamingMusicService.Pause(context);
	            break;
	        case KeyEvent.KEYCODE_HEADSETHOOK:
	        case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
	            if (StreamingMusicService.getPlaylist() != null && StreamingMusicService.getPlaylist().isPlaying())
	            	StreamingMusicService.Pause(context);
	            else
	            	StreamingMusicService.Play(context);
	            break;
	        case KeyEvent.KEYCODE_MEDIA_NEXT:
	            StreamingMusicService.Next(context);
	            break;
	        case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
	            StreamingMusicService.Previous(context);
	            break;
	    }
	}

}
