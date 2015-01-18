package com.lasthopesoftware.bluewater.servers.library.items.files.nowplaying.service.receivers;

import com.lasthopesoftware.bluewater.servers.library.items.files.nowplaying.service.NowPlayingService;

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
	            NowPlayingService.pause(context);
	            break;
	        case KeyEvent.KEYCODE_HEADSETHOOK:
	        case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
	            if (NowPlayingService.getPlaylistController() != null && NowPlayingService.getPlaylistController().isPlaying())
	            	NowPlayingService.pause(context);
	            else
	            	NowPlayingService.play(context);
	            break;
	        case KeyEvent.KEYCODE_MEDIA_NEXT:
	            NowPlayingService.next(context);
	            break;
	        case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
	            NowPlayingService.previous(context);
	            break;
	    }
	}

}
