package com.lasthopesoftware.bluewater.activities.ViewNowPlayingHelpers;

import java.io.IOException;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.activities.ViewNowPlaying;
import com.lasthopesoftware.bluewater.activities.WaitForConnectionDialog;
import com.lasthopesoftware.bluewater.data.objects.JrFile;
import com.lasthopesoftware.bluewater.data.objects.JrSession;

import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ProgressBar;

public class HandleViewNowPlayingMessages extends Handler {
//	private static int UPDATE_ALL = 0;
	public static final int UPDATE_PLAYING = 1;
//	private static int SET_STOPPED = 2;
	public static final int HIDE_CONTROLS = 3;
	public static final int SHOW_CONNECTION_LOST = 4;
	
	private ProgressBar mSongProgress;
	private ViewNowPlaying mOwner;
	private JrFile mFile;

	public HandleViewNowPlayingMessages(ViewNowPlaying owner, JrFile file) {
		super();
		
		mOwner = owner;
		mFile = file;
		mSongProgress = (ProgressBar) mOwner.findViewById(R.id.pbNowPlaying);
	}

	@Override
	public void handleMessage(Message msg) {
		switch (msg.arg1) {
		case SHOW_CONNECTION_LOST:
			WaitForConnectionDialog.show(mOwner);
			return;
		case UPDATE_PLAYING:
			if (mFile == null) return;
			
			try {
				mSongProgress.setMax(mFile.getDuration());
			} catch (IOException e) {
				WaitForConnectionDialog.show(mOwner);
			}
			mSongProgress.setProgress(mFile.getCurrentPosition());
			return;
		case HIDE_CONTROLS:
			mOwner.getControlNowPlaying().setVisibility(View.INVISIBLE);
			mOwner.getContentView().invalidate();
			return;
		}
	}
}
