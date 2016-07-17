package com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying;

import android.os.AsyncTask;
import android.os.Message;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackFile;
import com.vedsoft.lazyj.AbstractSynchronousLazy;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class NowPlayingActivityProgressTrackerTask extends AsyncTask<Void, Void, Void> {
	private static final ExecutorService trackerExecutor = Executors.newSingleThreadExecutor();

	private final IPlaybackFile filePlayer;

	private final NowPlayingActivityMessageHandler handler;

	private final AbstractSynchronousLazy<Integer> fileDuration = new AbstractSynchronousLazy<Integer>() {
		@Override
		protected final Integer initialize() {
			return filePlayer.getDuration();
		}
	};

	public static NowPlayingActivityProgressTrackerTask trackProgress(IPlaybackFile filePlayer, NowPlayingActivityMessageHandler handler) {
		final NowPlayingActivityProgressTrackerTask newProgressTrackerThread = new NowPlayingActivityProgressTrackerTask(filePlayer, handler);
		newProgressTrackerThread.executeOnExecutor(trackerExecutor);
		return newProgressTrackerThread;
	}
	
	private NowPlayingActivityProgressTrackerTask(IPlaybackFile filePlayer, NowPlayingActivityMessageHandler handler) {
		this.filePlayer = filePlayer;
		this.handler = handler;
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		while (!isCancelled() && filePlayer != null && filePlayer.isMediaPlayerCreated()) {
			try {
				
				if (filePlayer.isPlaying())
					handler.sendMessage(getUpdatePlayingMessage());
				
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				return null;
			}
		}
		
		return null;
	}
	
	private Message getUpdatePlayingMessage() {
		final Message msg = new Message();
		msg.what = NowPlayingActivityMessageHandler.UPDATE_PLAYING;
		msg.arg1 = filePlayer.getCurrentPosition();
		msg.arg2 = fileDuration.getObject();
		
		return msg;
	}
}