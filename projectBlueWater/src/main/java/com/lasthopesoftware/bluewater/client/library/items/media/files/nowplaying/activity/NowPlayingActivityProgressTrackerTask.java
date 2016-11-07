package com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.activity;

import android.os.AsyncTask;
import android.os.Message;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;
import com.vedsoft.lazyj.AbstractSynchronousLazy;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class NowPlayingActivityProgressTrackerTask extends AsyncTask<Void, Void, Void> {
	private static final ExecutorService trackerExecutor = Executors.newSingleThreadExecutor();

	private final NowPlayingActivityMessageHandler handler;

	private final IPlaybackHandler playbackHandler;

	private final AbstractSynchronousLazy<Integer> fileDuration = new AbstractSynchronousLazy<Integer>() {
		@Override
		protected final Integer initialize() {
			return playbackHandler.getDuration();
		}
	};

	static NowPlayingActivityProgressTrackerTask trackProgress(IPlaybackHandler playbackHandler, NowPlayingActivityMessageHandler handler) {
		final NowPlayingActivityProgressTrackerTask newProgressTrackerThread = new NowPlayingActivityProgressTrackerTask(playbackHandler, handler);
		newProgressTrackerThread.executeOnExecutor(trackerExecutor);
		return newProgressTrackerThread;
	}
	
	private NowPlayingActivityProgressTrackerTask(IPlaybackHandler playbackHandler, NowPlayingActivityMessageHandler handler) {
		this.playbackHandler = playbackHandler;
		this.handler = handler;
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		while (!isCancelled() && playbackHandler != null) {
			try {
				
				if (playbackHandler.isPlaying())
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
		msg.arg1 = playbackHandler.getCurrentPosition();
		msg.arg2 = fileDuration.getObject();
		
		return msg;
	}
}