package com.lasthopesoftware.bluewater.client.playback.service.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService;

public class RemoteControlReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		final KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
	    if (event.getAction() != KeyEvent.ACTION_UP) return;
	    
	    switch (event.getKeyCode()) {
	        case KeyEvent.KEYCODE_MEDIA_PLAY:
	        	PlaybackService.play(context);
	        	break;
			case KeyEvent.KEYCODE_MEDIA_STOP:
			case KeyEvent.KEYCODE_MEDIA_PAUSE:
	        	PlaybackService.pause(context);
	        	break;
	        case KeyEvent.KEYCODE_HEADSETHOOK:
	        case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
				PlaybackService.togglePlayPause(context);
	            break;
	        case KeyEvent.KEYCODE_MEDIA_NEXT:
	            PlaybackService.next(context);
	            break;
	        case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
	            PlaybackService.previous(context);
	            break;
	    }
	}
}
