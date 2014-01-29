package com.lasthopesoftware.bluewater.activities.ViewNowPlayingHelpers;

import android.os.Handler;
import android.os.Message;
import android.view.View;

import com.lasthopesoftware.bluewater.activities.ViewNowPlaying;
import com.lasthopesoftware.bluewater.activities.WaitForConnectionDialog;

public class HandleViewNowPlayingMessages extends Handler {
//	private static int UPDATE_ALL = 0;
	public static final int UPDATE_PLAYING = 1;
//	private static int SET_STOPPED = 2;
	public static final int HIDE_CONTROLS = 3;
	public static final int SHOW_CONNECTION_LOST = 4;
	
	private ViewNowPlaying mOwner;

	public HandleViewNowPlayingMessages(ViewNowPlaying owner) {
		super();
		
		mOwner = owner;
	}

	@Override
	public void handleMessage(Message msg) {
		switch (msg.what) {
		case UPDATE_PLAYING:
			mOwner.getSongProgressBar().setMax(msg.arg2);
			mOwner.getSongProgressBar().setProgress(msg.arg1);
			return;
		case HIDE_CONTROLS:
			mOwner.getControlNowPlaying().setVisibility(View.INVISIBLE);
			mOwner.getContentView().invalidate();
			return;
		}
	}
}
